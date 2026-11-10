package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosReceiptPrinterFacade @Inject constructor(
    private val receiptPrinterManager: ReceiptPrinterManager,
) {
    val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus> = receiptPrinterManager.connectionStatus

    fun discover(): Flow<ReceiptPrinterDevice> = receiptPrinterManager.discover()

    fun stopDiscovery() {
        receiptPrinterManager.stopDiscovery()
    }

    suspend fun connect(device: ReceiptPrinterDevice) {
        receiptPrinterManager.connect(device)
    }

    suspend fun disconnect() {
        receiptPrinterManager.disconnect()
    }

    suspend fun print(text: String) {
        receiptPrinterManager.print(text)
    }
}
