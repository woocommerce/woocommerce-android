package com.woocommerce.android.receiptprinter.star

import android.os.Build
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.PrinterDelegate
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarIO10CommunicationException
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarIO10IllegalHostDeviceStateException
import com.starmicronics.stario10.StarIO10InvalidOperationException
import com.starmicronics.stario10.StarIO10NotFoundException
import com.starmicronics.stario10.StarIO10UnprintableException
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterModel
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

internal class StarReceiptPrinterManager @Inject constructor(
    private val starIO10: StarIO10Wrapper,
) : ReceiptPrinterManager {
    private val _connectionStatus =
        MutableStateFlow<ReceiptPrinterConnectionStatus>(ReceiptPrinterConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus> = _connectionStatus.asStateFlow()

    private val discoveredPrinters = ConcurrentHashMap<String, StarPrinter>()
    private val sessionLock = Any()
    private var activeSession: DiscoverySession? = null

    private val connectionMutex = Mutex()
    private val attemptLock = Any()
    private var latestAttempt: Any? = null
    private var openConnection: Connection? = null
    private val connectionLossScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    init {
        starIO10.disableDiagnosticsUpload()
    }

    override fun discover(): Flow<ReceiptPrinterDiscoveryEvent> = callbackFlow {
        val session = DiscoverySession(starIO10, discoveredPrinters, channel)
        synchronized(sessionLock) {
            activeSession?.end()
            activeSession = session
            session.start(bluetoothInterfaceTypes())
        }
        val timeout = launch {
            delay(DISCOVERY_TIMEOUT_MS)
            session.fail()
        }
        awaitClose {
            timeout.cancel()
            session.stopDiscovery()
        }
    }.buffer(Channel.UNLIMITED).catch { cause ->
        if (cause !is DiscoveryFailedException) throw cause
        emit(ReceiptPrinterDiscoveryEvent.Failed(ReceiptPrinterError.DiscoveryFailure))
    }

    override suspend fun connect(device: ReceiptPrinterDevice): ReceiptPrinterResult {
        val caller = currentCoroutineContext()
        caller.ensureActive()
        val printer = discoveredPrinters[device.id]
            ?: return ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotFound)
        val attempt = startAttempt(ReceiptPrinterConnectionStatus.Connecting(device))
        // The SDK keeps opening after a cancellation, so the attempt always runs to the end to close what it opened.
        val result = withContext(NonCancellable) {
            connectionMutex.withLock {
                if (!isLatest(attempt)) {
                    return@withLock ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected)
                }
                closeOpenPrinter()
                val error = if (caller.isActive) open(printer, attempt) else ReceiptPrinterError.PrinterNotConnected
                val connected = error == null && caller.isActive &&
                    finishAttempt(attempt, ReceiptPrinterConnectionStatus.Connected(device))
                if (connected) {
                    ReceiptPrinterResult.Success
                } else {
                    closeOpenPrinter()
                    finishAttempt(attempt, ReceiptPrinterConnectionStatus.Disconnected)
                    ReceiptPrinterResult.Failure(error ?: ReceiptPrinterError.PrinterNotConnected)
                }
            }
        }
        if (result is ReceiptPrinterResult.Failure) caller.ensureActive()
        return result
    }

    override suspend fun disconnect() {
        val attempt = synchronized(attemptLock) {
            if (_connectionStatus.value == ReceiptPrinterConnectionStatus.Disconnected) return
            startAttempt(ReceiptPrinterConnectionStatus.Disconnecting)
        }
        closeAsLatest(attempt)
    }

    override suspend fun printReceipt(text: String): ReceiptPrinterResult {
        val (connection, error) = connectionMutex.withLock {
            val connection = openConnection
                ?: return ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected)
            val error = print(connection.printer, text) ?: return ReceiptPrinterResult.Success
            connection to error
        }
        if (error is ReceiptPrinterError.ConnectionLost) disconnectLost(connection.attempt)
        return ReceiptPrinterResult.Failure(error)
    }

    private suspend fun disconnectLost(connectAttempt: Any) {
        val attempt = synchronized(attemptLock) {
            // While the attempt is still connecting, the connect reports the outcome itself.
            val isCurrent = latestAttempt === connectAttempt &&
                _connectionStatus.value is ReceiptPrinterConnectionStatus.Connected
            if (!isCurrent) return
            startAttempt(ReceiptPrinterConnectionStatus.Disconnecting)
        }
        closeAsLatest(attempt)
    }

    private suspend fun closeAsLatest(attempt: Any) {
        withContext(NonCancellable) {
            connectionMutex.withLock {
                if (isLatest(attempt)) {
                    closeOpenPrinter()
                    finishAttempt(attempt, ReceiptPrinterConnectionStatus.Disconnected)
                }
            }
        }
    }

    private fun startAttempt(status: ReceiptPrinterConnectionStatus): Any = synchronized(attemptLock) {
        Any().also {
            latestAttempt = it
            _connectionStatus.value = status
        }
    }

    private fun isLatest(attempt: Any): Boolean = synchronized(attemptLock) { latestAttempt === attempt }

    private fun finishAttempt(attempt: Any, status: ReceiptPrinterConnectionStatus): Boolean =
        synchronized(attemptLock) {
            (latestAttempt === attempt).also { isLatest -> if (isLatest) _connectionStatus.value = status }
        }

    private suspend fun open(printer: StarPrinter, attempt: Any): ReceiptPrinterError? =
        try {
            printer.printerDelegate = object : PrinterDelegate() {
                override fun onCommunicationError(e: StarIO10Exception) {
                    if (e.toPrintError() !is ReceiptPrinterError.ConnectionLost) return
                    connectionLossScope.launch { disconnectLost(attempt) }
                }
            }
            printer.openAsync().await()
            openConnection = Connection(printer, attempt)
            null
        } catch (expected: StarIO10NotFoundException) {
            ReceiptPrinterError.PrinterNotFound
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            ReceiptPrinterError.ConnectionLost(cause)
        }

    private suspend fun closeOpenPrinter() {
        try {
            openConnection?.printer?.closeAsync()?.await()
            openConnection = null
        } catch (@Suppress("TooGenericExceptionCaught") expected: Exception) {
            return
        }
    }

    private suspend fun print(printer: StarPrinter, text: String): ReceiptPrinterError? =
        try {
            val content = PrinterBuilder().actionPrintText(text).actionFeedLine(2).actionCut(CutType.Partial)
            val command = StarXpandCommandBuilder().addDocument(DocumentBuilder().addPrinter(content)).getCommands()
            printer.printAsync(command).await()
            null
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            currentCoroutineContext().ensureActive()
            cause.toPrintError()
        }

    private fun bluetoothInterfaceTypes(): List<InterfaceType> =
        if (starIO10.androidSdkVersion >= Build.VERSION_CODES.S) {
            listOf(InterfaceType.Bluetooth, InterfaceType.BluetoothLE)
        } else {
            listOf(InterfaceType.Bluetooth)
        }

    private class Connection(val printer: StarPrinter, val attempt: Any)

    private class DiscoverySession(
        private val starIO10: StarIO10Wrapper,
        private val discoveredPrinters: MutableMap<String, StarPrinter>,
        private val events: SendChannel<ReceiptPrinterDiscoveryEvent>,
    ) : StarDeviceDiscoveryManager.Callback {
        private val lock = Any()
        private var discoveryManager: StarDeviceDiscoveryManager? = null

        fun start(interfaceTypes: List<InterfaceType>) {
            try {
                val manager = starIO10.createDiscoveryManager(interfaceTypes)
                synchronized(lock) { discoveryManager = manager }
                manager.discoveryTime = DISCOVERY_TIME_MS
                manager.callback = this
                manager.startDiscovery()
            } catch (expected: StarIO10Exception) {
                fail()
            } catch (expected: SecurityException) {
                fail()
            }
        }

        fun end() {
            stopDiscovery()
            events.close()
        }

        fun stopDiscovery() {
            val manager = synchronized(lock) { discoveryManager.also { discoveryManager = null } }
            try {
                manager?.stopDiscovery()
            } catch (expected: StarIO10Exception) {
                return
            } catch (expected: SecurityException) {
                return
            }
        }

        override fun onPrinterFound(printer: StarPrinter) {
            try {
                val device = synchronized(lock) {
                    if (discoveryManager == null) return
                    printer.toReceiptPrinterDevice().also { discoveredPrinters[it.id] = printer }
                }
                events.trySend(ReceiptPrinterDiscoveryEvent.PrinterFound(device))
            } catch (expected: SecurityException) {
                fail()
            }
        }

        override fun onDiscoveryFinished() {
            events.close()
        }

        fun fail() {
            events.close(DiscoveryFailedException())
            stopDiscovery()
        }
    }

    private companion object {
        const val DISCOVERY_TIME_MS = 30_000
        const val DISCOVERY_TIMEOUT_MS = DISCOVERY_TIME_MS + 5_000L
    }
}

private class DiscoveryFailedException : Exception()

private fun Exception.toPrintError(): ReceiptPrinterError = when (this) {
    is StarIO10UnprintableException -> when {
        status?.coverOpen == true -> ReceiptPrinterError.CoverOpen
        status?.paperEmpty == true -> ReceiptPrinterError.PaperEmpty
        else -> ReceiptPrinterError.PrintFailed(this)
    }
    is StarIO10CommunicationException,
    is StarIO10IllegalHostDeviceStateException,
    is StarIO10NotFoundException,
    is StarIO10InvalidOperationException -> ReceiptPrinterError.ConnectionLost(this)
    else -> ReceiptPrinterError.PrintFailed(this)
}

private fun StarPrinter.toReceiptPrinterDevice(): ReceiptPrinterDevice {
    val identifier = connectionSettings.identifier
    val bluetoothName = information?.detail?.let { detail ->
        when (connectionSettings.interfaceType) {
            InterfaceType.Bluetooth -> detail.bluetooth.deviceName
            InterfaceType.BluetoothLE -> detail.bluetoothLE.deviceName
            else -> null
        }
    }
    val modelName = information?.model?.takeIf { it != StarPrinterModel.Unknown }?.modelName
    val name = listOfNotNull(bluetoothName, modelName).firstOrNull { it.isNotBlank() } ?: identifier
    return ReceiptPrinterDevice(id = identifier, name = name)
}
