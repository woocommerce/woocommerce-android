package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterError {
    data object DiscoveryFailure : ReceiptPrinterError
    data object PrinterNotFound : ReceiptPrinterError
    data object PrinterNotConnected : ReceiptPrinterError
    data object PaperEmpty : ReceiptPrinterError
    data object CoverOpen : ReceiptPrinterError
    data class ConnectionLost(val cause: Throwable? = null) : ReceiptPrinterError
    data class PrintFailed(val cause: Throwable? = null) : ReceiptPrinterError
}
