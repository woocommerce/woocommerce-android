package com.woocommerce.android.ui.woopos.home.totals

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BuildAllPaymentMethodsTest {
    private val readyForPayment = WooPosTotalsViewState.ReaderStatus.ReadyForPayment("ready", "tap")
    private val disconnected = WooPosTotalsViewState.ReaderStatus.Disconnected("title", "subtitle", "cta")

    @Test
    fun `given reader connected and TTP available, when building methods, then list is TapToPay, ScanToPay, MarkOrderAsPaid`() {
        val methods = buildAllPaymentMethods(readyForPayment, isTapToPayAvailable = true)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.TAP_TO_PAY,
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader connected and TTP unavailable, when building methods, then list is ScanToPay, MarkOrderAsPaid`() {
        val methods = buildAllPaymentMethods(readyForPayment, isTapToPayAvailable = false)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader disconnected and TTP available, when building methods, then list is CardReader, ScanToPay, MarkOrderAsPaid`() {
        val methods = buildAllPaymentMethods(disconnected, isTapToPayAvailable = true)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.CARD_READER,
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader disconnected and TTP unavailable, when building methods, then list is ScanToPay, MarkOrderAsPaid`() {
        val methods = buildAllPaymentMethods(disconnected, isTapToPayAvailable = false)

        assertThat(methods).containsExactly(
            WooPosPaymentMethod.SCAN_TO_PAY,
            WooPosPaymentMethod.MARK_ORDER_AS_PAID,
        )
    }

    @Test
    fun `given reader Preparing and TTP available, when building methods, then TapToPay is included`() {
        val preparing = WooPosTotalsViewState.ReaderStatus.Preparing("title", "subtitle")

        val methods = buildAllPaymentMethods(preparing, isTapToPayAvailable = true)

        assertThat(methods).contains(WooPosPaymentMethod.TAP_TO_PAY)
        assertThat(methods).doesNotContain(WooPosPaymentMethod.CARD_READER)
    }

    @Test
    fun `given reader Unavailable and TTP available, when building methods, then CardReader is included`() {
        val methods = buildAllPaymentMethods(
            WooPosTotalsViewState.ReaderStatus.Unavailable,
            isTapToPayAvailable = true,
        )

        assertThat(methods).contains(WooPosPaymentMethod.CARD_READER)
        assertThat(methods).doesNotContain(WooPosPaymentMethod.TAP_TO_PAY)
    }
}
