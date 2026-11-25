package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.R
import com.woocommerce.android.receiptprinter.ReceiptPrinterError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosPrintReceiptFailureTest {
    @Test
    fun `when the printer is out of paper, then the failure is paper empty`() {
        assertThat(WooPosPrintReceiptFailure.from(ReceiptPrinterError.PaperEmpty))
            .isEqualTo(WooPosPrintReceiptFailure.PaperEmpty)
    }

    @Test
    fun `when the printer cover is open, then the failure is cover open`() {
        assertThat(WooPosPrintReceiptFailure.from(ReceiptPrinterError.CoverOpen))
            .isEqualTo(WooPosPrintReceiptFailure.CoverOpen)
    }

    @Test
    fun `when the connection is lost, then the failure is printer offline`() {
        assertThat(WooPosPrintReceiptFailure.from(ReceiptPrinterError.ConnectionLost()))
            .isEqualTo(WooPosPrintReceiptFailure.PrinterOffline)
    }

    @Test
    fun `when the printer is not connected, then the failure is printer offline`() {
        assertThat(WooPosPrintReceiptFailure.from(ReceiptPrinterError.PrinterNotConnected))
            .isEqualTo(WooPosPrintReceiptFailure.PrinterOffline)
    }

    @Test
    fun `when the error is any other, then the failure is generic`() {
        val otherErrors = listOf(
            ReceiptPrinterError.PrintFailed(),
            ReceiptPrinterError.DiscoveryFailure,
            ReceiptPrinterError.PrinterNotFound,
        )

        assertThat(otherErrors.map { WooPosPrintReceiptFailure.from(it) })
            .containsOnly(WooPosPrintReceiptFailure.Generic)
    }

    @Test
    fun `when the failure is paper empty, then it asks to check the paper`() {
        assertThat(WooPosPrintReceiptFailure.PaperEmpty.title)
            .isEqualTo(R.string.woopos_print_receipt_failure_paper_empty_title)
        assertThat(WooPosPrintReceiptFailure.PaperEmpty.message)
            .isEqualTo(R.string.woopos_print_receipt_failure_paper_empty_message)
    }

    @Test
    fun `when the failure is cover open, then it asks to close the cover`() {
        assertThat(WooPosPrintReceiptFailure.CoverOpen.title)
            .isEqualTo(R.string.woopos_print_receipt_failure_cover_open_title)
        assertThat(WooPosPrintReceiptFailure.CoverOpen.message)
            .isEqualTo(R.string.woopos_print_receipt_failure_cover_open_message)
    }

    @Test
    fun `when the failure is printer offline, then it says the printer cannot be reached`() {
        assertThat(WooPosPrintReceiptFailure.PrinterOffline.title)
            .isEqualTo(R.string.woopos_print_receipt_failure_printer_offline_title)
        assertThat(WooPosPrintReceiptFailure.PrinterOffline.message)
            .isEqualTo(R.string.woopos_print_receipt_failure_printer_offline_message)
    }

    @Test
    fun `when the failure is generic, then it says the receipt could not be printed`() {
        assertThat(WooPosPrintReceiptFailure.Generic.title)
            .isEqualTo(R.string.woopos_print_receipt_failure_generic_title)
        assertThat(WooPosPrintReceiptFailure.Generic.message)
            .isEqualTo(R.string.woopos_print_receipt_failure_generic_message)
    }
}
