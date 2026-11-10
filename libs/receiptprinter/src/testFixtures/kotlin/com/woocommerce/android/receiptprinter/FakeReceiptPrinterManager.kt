package com.woocommerce.android.receiptprinter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class FakeReceiptPrinterManager(
    initialStatus: ReceiptPrinterConnectionStatus = ReceiptPrinterConnectionStatus.Idle,
) : ReceiptPrinterManager {
    private val _connectionStatus = MutableStateFlow(initialStatus)
    override val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus> = _connectionStatus.asStateFlow()

    var discoveredDevices: List<ReceiptPrinterDevice> = emptyList()
    var discoveryError: ReceiptPrinterError? = null
    var connectError: ReceiptPrinterError? = null
    var printError: ReceiptPrinterError? = null

    val printedTexts: MutableList<String> = mutableListOf()
    var isDiscovering: Boolean = false
        private set
    var disconnectCalls: Int = 0
        private set

    override fun discover(): Flow<ReceiptPrinterDevice> = flow {
        isDiscovering = true
        discoveryError?.let { throw ReceiptPrinterException(it) }
        discoveredDevices.forEach { emit(it) }
    }

    override fun stopDiscovery() {
        isDiscovering = false
    }

    override suspend fun connect(device: ReceiptPrinterDevice) {
        _connectionStatus.value = ReceiptPrinterConnectionStatus.Connecting
        connectError?.let {
            _connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnected
            throw ReceiptPrinterException(it)
        }
        _connectionStatus.value = ReceiptPrinterConnectionStatus.Connected(device)
    }

    override suspend fun disconnect() {
        disconnectCalls++
        _connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnecting
        _connectionStatus.value = ReceiptPrinterConnectionStatus.Disconnected
    }

    override suspend fun print(text: String) {
        if (connectionStatus.value !is ReceiptPrinterConnectionStatus.Connected) {
            throw ReceiptPrinterException(ReceiptPrinterError.PrinterNotConnected)
        }
        printError?.let { throw ReceiptPrinterException(it) }
        printedTexts += text
    }

    fun setConnectionStatus(status: ReceiptPrinterConnectionStatus) {
        _connectionStatus.value = status
    }
}
