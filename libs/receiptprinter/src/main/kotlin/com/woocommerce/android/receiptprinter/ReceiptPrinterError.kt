package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterError {
    data object DiscoveryFailure : ReceiptPrinterError
    data object PrinterNotFound : ReceiptPrinterError
    data object PrinterNotConnected : ReceiptPrinterError
    data object PaperEmpty : ReceiptPrinterError
    data object CoverOpen : ReceiptPrinterError
    data class ConnectionLost(val detail: String?) : ReceiptPrinterError
    data class PrintFailed(val detail: String?) : ReceiptPrinterError
}

class ReceiptPrinterException(
    val error: ReceiptPrinterError,
    cause: Throwable? = null,
) : Exception("Receipt printer error: $error", cause)
