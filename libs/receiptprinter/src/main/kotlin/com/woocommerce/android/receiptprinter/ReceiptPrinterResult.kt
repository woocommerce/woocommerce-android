package com.woocommerce.android.receiptprinter

sealed interface ReceiptPrinterResult {
    data object Success : ReceiptPrinterResult
    data class Failure(val error: ReceiptPrinterError) : ReceiptPrinterResult
}
