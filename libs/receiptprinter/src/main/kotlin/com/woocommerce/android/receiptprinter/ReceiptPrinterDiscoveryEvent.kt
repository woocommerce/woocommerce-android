package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterDiscoveryEvent {
    data class PrinterFound(val device: ReceiptPrinterDevice) : ReceiptPrinterDiscoveryEvent
    data class Failed(val error: ReceiptPrinterError) : ReceiptPrinterDiscoveryEvent
}
