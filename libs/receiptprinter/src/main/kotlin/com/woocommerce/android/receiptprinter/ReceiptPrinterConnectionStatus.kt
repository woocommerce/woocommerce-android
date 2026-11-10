package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterConnectionStatus {
    data object Idle : ReceiptPrinterConnectionStatus
    data object Disconnected : ReceiptPrinterConnectionStatus
    data object Connecting : ReceiptPrinterConnectionStatus
    data class Connected(val device: ReceiptPrinterDevice) : ReceiptPrinterConnectionStatus
    data object Disconnecting : ReceiptPrinterConnectionStatus
}
