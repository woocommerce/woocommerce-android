package com.woocommerce.android.ui.woopos.home.totals

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BuildAllPaymentMethodsTest {
    private val readyForPayment = WooPosTotalsViewState.ReaderStatus.ReadyForPayment("ready", "tap")
    private val disconnected = WooPosTotalsViewState.ReaderStatus.Disconnected("title", "subtitle", "cta")

    @Test
    fun `given reader connected and TTP available, when building methods, then list is TapToPay, ScanToPay, MarkOrderAsPaid`() {
        val methods = build(readerStatus = readyForPayment, isTapToPayAvailable = true)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.TAP_TO_PAY,
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader connected and TTP unavailable, when building methods, then list is ScanToPay, MarkOrderAsPaid`() {
        val methods = build(readerStatus = readyForPayment, isTapToPayAvailable = false)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader disconnected and TTP available, when building methods, then list is CardReader, ScanToPay, MarkOrderAsPaid`() {
        val methods = build(readerStatus = disconnected, isTapToPayAvailable = true)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.CARD_READER,
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader disconnected and TTP unavailable, when building methods, then list is ScanToPay, MarkOrderAsPaid`() {
        val methods = build(readerStatus = disconnected, isTapToPayAvailable = false)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader Preparing and TTP available, when building methods, then TapToPay is included`() {
        val preparing = WooPosTotalsViewState.ReaderStatus.Preparing("title", "subtitle")

        val methods = build(readerStatus = preparing, isTapToPayAvailable = true)

        assertThat(methods).contains(WooPosPaymentMethod.TAP_TO_PAY)
        assertThat(methods).doesNotContain(WooPosPaymentMethod.CARD_READER)
    }

    @Test
    fun `given reader Unavailable and TTP available, when building methods, then CardReader is included`() {
        val methods = build(
            readerStatus = WooPosTotalsViewState.ReaderStatus.Unavailable,
            isTapToPayAvailable = true,
        )

        assertThat(methods).contains(WooPosPaymentMethod.CARD_READER)
        assertThat(methods).doesNotContain(WooPosPaymentMethod.TAP_TO_PAY)
    }

    @Test
    fun `given ScanToPay disabled, when building methods, then ScanToPay is not included`() {
        val methods = build(
            readerStatus = disconnected,
            isTapToPayAvailable = false,
            isScanToPayEnabled = false,
        )

        assertThat(methods).doesNotContain(WooPosPaymentMethod.SCAN_TO_PAY)
        assertThat(methods).contains(WooPosPaymentMethod.MARK_ORDER_AS_PAID)
    }

    @Test
    fun `given MarkOrderAsPaid disabled, when building methods, then MarkOrderAsPaid is not included`() {
        val methods = build(
            readerStatus = disconnected,
            isTapToPayAvailable = false,
            isMarkOrderAsPaidEnabled = false,
        )

        assertThat(methods).doesNotContain(WooPosPaymentMethod.MARK_ORDER_AS_PAID)
        assertThat(methods).contains(WooPosPaymentMethod.SCAN_TO_PAY)
    }

    @Test
    fun `given both flags disabled, when building methods, then neither new method is included`() {
        val methods = build(
            readerStatus = disconnected,
            isTapToPayAvailable = false,
            isScanToPayEnabled = false,
            isMarkOrderAsPaidEnabled = false,
        )

        assertThat(methods).doesNotContain(WooPosPaymentMethod.SCAN_TO_PAY)
        assertThat(methods).doesNotContain(WooPosPaymentMethod.MARK_ORDER_AS_PAID)
    }

    private fun build(
        readerStatus: WooPosTotalsViewState.ReaderStatus,
        isTapToPayAvailable: Boolean,
        isScanToPayEnabled: Boolean = true,
        isMarkOrderAsPaidEnabled: Boolean = true,
    ) = buildAllPaymentMethods(
        readerStatus = readerStatus,
        isTapToPayAvailable = isTapToPayAvailable,
        isScanToPayEnabled = isScanToPayEnabled,
        isMarkOrderAsPaidEnabled = isMarkOrderAsPaidEnabled,
    )
}
