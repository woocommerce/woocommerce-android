package com.woocommerce.android.receiptprinter.star

import android.os.Build
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterModel
import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
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

    override suspend fun connect(device: ReceiptPrinterDevice): ReceiptPrinterResult =
        ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected)

    override suspend fun disconnect() = Unit

    override suspend fun printReceipt(text: String): ReceiptPrinterResult =
        ReceiptPrinterResult.Failure(ReceiptPrinterError.PrinterNotConnected)

    private fun bluetoothInterfaceTypes(): List<InterfaceType> =
        if (starIO10.androidSdkVersion >= Build.VERSION_CODES.S) {
            listOf(InterfaceType.Bluetooth, InterfaceType.BluetoothLE)
        } else {
            listOf(InterfaceType.Bluetooth)
        }

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
            stopDiscovery()
            events.close(DiscoveryFailedException())
        }
    }

    private companion object {
        const val DISCOVERY_TIME_MS = 30_000
        const val DISCOVERY_TIMEOUT_MS = DISCOVERY_TIME_MS + 5_000L
    }
}

private class DiscoveryFailedException : Exception()

private fun StarPrinter.toReceiptPrinterDevice(): ReceiptPrinterDevice {
    val identifier = connectionSettings.identifier
    val bluetoothName = information?.detail?.let { detail ->
        when (connectionSettings.interfaceType) {
            InterfaceType.BluetoothLE -> detail.bluetoothLE.deviceName
            else -> detail.bluetooth.deviceName
        }
    }
    val modelName = information?.model?.takeIf { it != StarPrinterModel.Unknown }?.modelName
    val name = listOfNotNull(bluetoothName, modelName).firstOrNull { it.isNotBlank() } ?: identifier
    return ReceiptPrinterDevice(id = identifier, name = name)
}
