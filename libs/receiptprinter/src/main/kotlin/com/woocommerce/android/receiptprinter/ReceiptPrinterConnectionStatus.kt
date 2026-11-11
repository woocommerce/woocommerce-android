package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterConnectionStatus {
    data object Disconnected : ReceiptPrinterConnectionStatus
    data class Connecting(val device: ReceiptPrinterDevice) : ReceiptPrinterConnectionStatus
    data class Connected(val device: ReceiptPrinterDevice) : ReceiptPrinterConnectionStatus
    data object Disconnecting : ReceiptPrinterConnectionStatus
}
