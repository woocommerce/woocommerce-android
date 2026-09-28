package com.woocommerce.android.ui.woopos.cashdrawer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.DrawerDelegate
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarDeviceDiscoveryManagerFactory
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.DrawerBuilder
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.drawer.OpenParameter
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class WooPosReceiptPrinter(val interfaceType: String, val identifier: String, val model: String?)

class WooPosPrinterNotConnectedException : IllegalStateException("No receipt printer is connected")

interface WooPosCashDrawerHardware {
    val isConnected: StateFlow<Boolean>
    val selectedPrinter: StateFlow<WooPosReceiptPrinter?>
    val drawerSignals: SharedFlow<Boolean>
    suspend fun discover(): List<WooPosReceiptPrinter>
    suspend fun connect(printer: WooPosReceiptPrinter)
    suspend fun disconnect()
    suspend fun open()
    suspend fun printCloseOut(text: String)
}

@Singleton
class WooPosStarReceiptPrinter @Inject constructor(@ApplicationContext private val context: Context) : WooPosCashDrawerHardware {
    private val preferences = context.getSharedPreferences("woo_pos_receipt_printer", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activePrinter: StarPrinter? = null
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected
    private val _selectedPrinter = MutableStateFlow(loadSelection())
    override val selectedPrinter: StateFlow<WooPosReceiptPrinter?> = _selectedPrinter
    private val _drawerSignals = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 16)
    override val drawerSignals: SharedFlow<Boolean> = _drawerSignals

    init {
        if (_selectedPrinter.value != null) {
            connectionScope.launch {
                mutex.withLock {
                    try {
                        requireActivePrinter()
                    } catch (_: Exception) {
                        _isConnected.value = false
                    }
                }
            }
        }
    }

    override suspend fun discover(): List<WooPosReceiptPrinter> = suspendCancellableCoroutine { continuation ->
        val found = CopyOnWriteArrayList<WooPosReceiptPrinter>()
        try {
            val manager = StarDeviceDiscoveryManagerFactory.create(
                listOf(InterfaceType.Lan, InterfaceType.Bluetooth, InterfaceType.BluetoothLE, InterfaceType.Usb), context
            )
            manager.discoveryTime = DISCOVERY_MS
            manager.callback = object : StarDeviceDiscoveryManager.Callback {
                override fun onPrinterFound(printer: StarPrinter) {
                    val selection = WooPosReceiptPrinter(
                        printer.connectionSettings.interfaceType.toString(),
                        printer.connectionSettings.identifier,
                        printer.information?.model?.toString(),
                    )
                    if (!found.contains(selection)) found.add(selection)
                }

                override fun onDiscoveryFinished() {
                    if (continuation.isActive) continuation.resume(found.toList())
                }
            }
            manager.startDiscovery()
            continuation.invokeOnCancellation { manager.stopDiscovery() }
        } catch (error: Exception) {
            if (continuation.isActive) continuation.cancel(error)
        }
    }

    override suspend fun connect(printer: WooPosReceiptPrinter) {
        mutex.withLock {
            closeActivePrinter()
            val connectedPrinter = createPrinter(printer)
            try {
                connectedPrinter.openAsync().await()
                emitCurrentDrawerSignal(connectedPrinter)
            } catch (error: Exception) {
                _isConnected.value = false
                throw error
            }
            activePrinter = connectedPrinter
            preferences.edit().putString(KEY_INTERFACE, printer.interfaceType)
                .putString(KEY_IDENTIFIER, printer.identifier).putString(KEY_MODEL, printer.model).apply()
            _selectedPrinter.value = printer
            _isConnected.value = true
        }
    }

    override suspend fun disconnect() {
        mutex.withLock {
            closeActivePrinter()
            preferences.edit().clear().apply()
            _selectedPrinter.value = null
            _isConnected.value = false
        }
    }

    override suspend fun open() {
        val command = StarXpandCommandBuilder().addDocument(
            DocumentBuilder().addDrawer(DrawerBuilder().actionOpen(OpenParameter()))
        ).getCommands()
        execute(command)
    }

    override suspend fun printCloseOut(text: String) {
        val layout = createTextLayout(text)
        mutex.withLock {
            try {
                val printer = requireActivePrinter()
                var offset = 0
                do {
                    val pageHeight = minOf(PRINT_PAGE_HEIGHT, layout.height - offset).coerceAtLeast(1)
                    val bitmap = renderPage(layout, offset, pageHeight)
                    try {
                        val builder = PrinterBuilder().actionPrintImage(ImageParameter(bitmap, PRINT_WIDTH))
                        if (offset + pageHeight >= layout.height) builder.actionCut(CutType.Partial)
                        val command = StarXpandCommandBuilder().addDocument(
                            DocumentBuilder().addPrinter(builder)
                        ).getCommands()
                        printer.printAsync(command).await()
                    } finally {
                        bitmap.recycle()
                    }
                    offset += pageHeight
                } while (offset < layout.height)
                _isConnected.value = true
            } catch (error: Exception) {
                closeActivePrinter()
                throw error
            }
        }
    }

    private suspend fun execute(command: String) {
        mutex.withLock {
            try {
                requireActivePrinter().printAsync(command).await()
                _isConnected.value = true
            } catch (error: Exception) {
                closeActivePrinter()
                _isConnected.value = false
                throw error
            }
        }
    }

    private suspend fun requireActivePrinter(): StarPrinter {
        activePrinter?.let { return it }
        val selection = _selectedPrinter.value ?: throw WooPosPrinterNotConnectedException()
        val printer = createPrinter(selection)
        printer.openAsync().await()
        emitCurrentDrawerSignal(printer)
        activePrinter = printer
        _isConnected.value = true
        return printer
    }

    private fun createPrinter(selection: WooPosReceiptPrinter): StarPrinter =
        StarPrinter(StarConnectionSettings(interfaceType(selection.interfaceType), selection.identifier), context).apply {
            drawerDelegate = object : DrawerDelegate() {
                override fun onOpenCloseSignalSwitched(openCloseSignal: Boolean) {
                    _drawerSignals.tryEmit(openCloseSignal)
                }
            }
        }

    private suspend fun emitCurrentDrawerSignal(printer: StarPrinter) {
        try {
            _drawerSignals.tryEmit(printer.getStatusAsync().await().drawerOpenCloseSignal)
        } catch (_: Exception) {
            // Some printers do not report a drawer sensor. Printing and opening can still work.
        }
    }

    private suspend fun closeActivePrinter() {
        val printer = activePrinter ?: return
        activePrinter = null
        try {
            printer.closeAsync().await()
        } catch (_: Exception) {
            // A lost connection must not prevent selecting another printer.
        }
        _isConnected.value = false
    }

    private fun loadSelection(): WooPosReceiptPrinter? {
        val type = preferences.getString(KEY_INTERFACE, null) ?: return null
        val identifier = preferences.getString(KEY_IDENTIFIER, null) ?: return null
        return WooPosReceiptPrinter(type, identifier, preferences.getString(KEY_MODEL, null))
    }

    private fun interfaceType(value: String): InterfaceType = when (value) {
        InterfaceType.Lan.toString() -> InterfaceType.Lan
        InterfaceType.Bluetooth.toString() -> InterfaceType.Bluetooth
        InterfaceType.BluetoothLE.toString() -> InterfaceType.BluetoothLE
        InterfaceType.Usb.toString() -> InterfaceType.Usb
        else -> throw IllegalArgumentException("Unsupported printer interface: $value")
    }

    private fun createTextLayout(text: String): StaticLayout {
        val paint = TextPaint().apply { color = Color.BLACK; textSize = 24f; isAntiAlias = true }
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, PRINT_WIDTH - 32)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
    }

    private fun renderPage(layout: StaticLayout, offset: Int, pageHeight: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(PRINT_WIDTH, pageHeight + 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate(16f, 16f - offset)
        layout.draw(canvas)
        return bitmap
    }

    companion object {
        private const val KEY_INTERFACE = "interface"
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_MODEL = "model"
        private const val DISCOVERY_MS = 10_000
        private const val PRINT_WIDTH = 384
        private const val PRINT_PAGE_HEIGHT = 1_600
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WooPosCashDrawerHardwareModule {
    @Binds
    abstract fun bindCashDrawerHardware(implementation: WooPosStarReceiptPrinter): WooPosCashDrawerHardware
}
