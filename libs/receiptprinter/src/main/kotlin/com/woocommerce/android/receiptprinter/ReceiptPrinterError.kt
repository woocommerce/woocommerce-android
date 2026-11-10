package com.woocommerce.android.receiptprinter

sealed class ReceiptPrinterError {
    data object DiscoveryFailure : ReceiptPrinterError()
    data object PrinterNotFound : ReceiptPrinterError()
    data object PrinterNotConnected : ReceiptPrinterError()
    data object PaperEmpty : ReceiptPrinterError()
    data object CoverOpen : ReceiptPrinterError()
    data object ConnectionLost : ReceiptPrinterError()
    data object PrintFailed : ReceiptPrinterError()
}

class ReceiptPrinterException(
    val error: ReceiptPrinterError,
    cause: Throwable? = null,
) : Exception("Receipt printer error: $error", cause)
