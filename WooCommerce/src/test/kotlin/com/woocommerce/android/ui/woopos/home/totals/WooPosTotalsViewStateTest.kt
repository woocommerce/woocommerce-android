package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.ReaderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosTotalsViewStateTest {
    @Test
    fun `when the reader prepares, checks the order or waits for a card, then a payment is in progress`() {
        listOf(
            ReaderStatus.Preparing("", ""),
            ReaderStatus.CheckingOrder("", ""),
            ReaderStatus.ReadyForPayment("", ""),
        ).forEach { status ->
            assertThat(checkout(status).isPaymentInProgress).isTrue()
        }
    }

    @Test
    fun `when Tap to Pay is starting, then a payment is in progress`() {
        val state = checkout(ReaderStatus.Unavailable, WooPosTotalsViewState.TapToPayProgress.Preparing)

        assertThat(state.isPaymentInProgress).isTrue()
    }

    @Test
    fun `when the card is being processed, then a payment is in progress`() {
        assertThat(WooPosTotalsViewState.PaymentInProgress("", "").isPaymentInProgress).isTrue()
    }

    @Test
    fun `when no reader is collecting, then no payment is in progress`() {
        listOf(
            checkout(ReaderStatus.Disconnected("", "", "")),
            checkout(ReaderStatus.Unavailable),
            WooPosTotalsViewState.PaymentSuccess(""),
            WooPosTotalsViewState.PaymentFailed("", "", ""),
            WooPosTotalsViewState.Idle,
        ).forEach { state ->
            assertThat(state.isPaymentInProgress).isFalse()
        }
    }

    private fun checkout(
        readerStatus: ReaderStatus,
        tapToPayProgress: WooPosTotalsViewState.TapToPayProgress? = null,
    ) = WooPosTotalsViewState.Checkout(
        totals = WooPosTotalsViewState.Totals.Hidden,
        readerStatus = readerStatus,
        tapToPayProgress = tapToPayProgress,
    )
}
