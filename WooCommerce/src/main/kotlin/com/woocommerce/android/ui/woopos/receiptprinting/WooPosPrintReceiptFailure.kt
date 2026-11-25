package com.woocommerce.android.ui.woopos.receiptprinting

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.receiptprinter.ReceiptPrinterError

enum class WooPosPrintReceiptFailure(
    @StringRes val title: Int,
    @StringRes val message: Int,
) {
    PaperEmpty(
        title = R.string.woopos_print_receipt_failure_paper_empty_title,
        message = R.string.woopos_print_receipt_failure_paper_empty_message,
    ),
    CoverOpen(
        title = R.string.woopos_print_receipt_failure_cover_open_title,
        message = R.string.woopos_print_receipt_failure_cover_open_message,
    ),
    PrinterOffline(
        title = R.string.woopos_print_receipt_failure_printer_offline_title,
        message = R.string.woopos_print_receipt_failure_printer_offline_message,
    ),
    Generic(
        title = R.string.woopos_print_receipt_failure_generic_title,
        message = R.string.woopos_print_receipt_failure_generic_message,
    );

    companion object {
        fun from(error: ReceiptPrinterError): WooPosPrintReceiptFailure = when (error) {
            ReceiptPrinterError.PaperEmpty -> PaperEmpty
            ReceiptPrinterError.CoverOpen -> CoverOpen
            is ReceiptPrinterError.ConnectionLost,
            ReceiptPrinterError.PrinterNotConnected -> PrinterOffline

            ReceiptPrinterError.DiscoveryFailure,
            ReceiptPrinterError.PrinterNotFound,
            is ReceiptPrinterError.PrintFailed -> Generic
        }
    }
}

enum class WooPosPrintReceiptFailureAction { Retry, EmailReceipt }
