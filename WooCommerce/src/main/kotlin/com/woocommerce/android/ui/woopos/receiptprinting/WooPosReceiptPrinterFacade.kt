package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.receiptprinter.ReceiptPrinterConnectionStatus
import com.woocommerce.android.receiptprinter.ReceiptPrinterDevice
import com.woocommerce.android.receiptprinter.ReceiptPrinterDiscoveryEvent
import com.woocommerce.android.receiptprinter.ReceiptPrinterManager
import com.woocommerce.android.receiptprinter.ReceiptPrinterResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosReceiptPrinterFacade @Inject constructor(
    private val receiptPrinterManager: ReceiptPrinterManager,
) {
    val connectionStatus: StateFlow<ReceiptPrinterConnectionStatus> = receiptPrinterManager.connectionStatus

    fun discover(): Flow<ReceiptPrinterDiscoveryEvent> = receiptPrinterManager.discover()

    suspend fun connect(device: ReceiptPrinterDevice): ReceiptPrinterResult = receiptPrinterManager.connect(device)

    suspend fun disconnect() {
        receiptPrinterManager.disconnect()
    }

    suspend fun printReceipt(text: String): ReceiptPrinterResult = receiptPrinterManager.printReceipt(text)
}
