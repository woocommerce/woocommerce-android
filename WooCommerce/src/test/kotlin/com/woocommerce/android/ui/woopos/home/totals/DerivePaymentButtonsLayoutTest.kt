package com.woocommerce.android.ui.woopos.home.totals

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DerivePaymentButtonsLayoutTest {
    private val connected = WooPosTotalsViewState.ReaderStatus.ReadyForPayment("ready", "tap")
    private val disconnected = WooPosTotalsViewState.ReaderStatus.Disconnected("title", "subtitle", "cta")

    @Test
    fun `given phone with reader connected and ttp available, when deriving layout, then primary is TTP and secondary is Cash`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.PHONE, connected, isTapToPayAvailable = true)
        assertThat(layout).isEqualTo(
            WooPosPaymentButtonsLayout.Pair(
                WooPosPaymentMethod.TAP_TO_PAY,
                WooPosPaymentMethod.CASH
            )
        )
    }

    @Test
    fun `given phone with reader connected and ttp unavailable, when deriving layout, then a single Cash button is returned`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.PHONE, connected, isTapToPayAvailable = false)
        assertThat(layout).isEqualTo(WooPosPaymentButtonsLayout.Single(WooPosPaymentMethod.CASH))
    }

    @Test
    fun `given phone with reader disconnected and ttp available, when deriving layout, then primary is TTP with overflow secondary`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.PHONE, disconnected, isTapToPayAvailable = true)
        assertThat(layout).isEqualTo(WooPosPaymentButtonsLayout.WithOverflow(WooPosPaymentMethod.TAP_TO_PAY))
    }

    @Test
    fun `given phone with reader disconnected and ttp unavailable, when deriving layout, then primary is CardReader and secondary is Cash`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.PHONE, disconnected, isTapToPayAvailable = false)
        assertThat(layout).isEqualTo(
            WooPosPaymentButtonsLayout.Pair(
                WooPosPaymentMethod.CARD_READER,
                WooPosPaymentMethod.CASH
            )
        )
    }

    @Test
    fun `given tablet with reader connected and ttp available, when deriving layout, then primary is Cash and secondary is TTP`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.TABLET, connected, isTapToPayAvailable = true)
        assertThat(layout).isEqualTo(
            WooPosPaymentButtonsLayout.Pair(
                WooPosPaymentMethod.CASH,
                WooPosPaymentMethod.TAP_TO_PAY
            )
        )
    }

    @Test
    fun `given tablet with reader connected and ttp unavailable, when deriving layout, then a single Cash button is returned`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.TABLET, connected, isTapToPayAvailable = false)
        assertThat(layout).isEqualTo(WooPosPaymentButtonsLayout.Single(WooPosPaymentMethod.CASH))
    }

    @Test
    fun `given tablet with reader disconnected and ttp available, when deriving layout, then primary is CardReader with overflow secondary`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.TABLET, disconnected, isTapToPayAvailable = true)
        assertThat(layout).isEqualTo(WooPosPaymentButtonsLayout.WithOverflow(WooPosPaymentMethod.CARD_READER))
    }

    @Test
    fun `given tablet with reader disconnected and ttp unavailable, when deriving layout, then primary is CardReader and secondary is Cash`() {
        val layout = derivePaymentButtonsLayout(WooPosFormFactor.TABLET, disconnected, isTapToPayAvailable = false)
        assertThat(layout).isEqualTo(
            WooPosPaymentButtonsLayout.Pair(
                WooPosPaymentMethod.CARD_READER,
                WooPosPaymentMethod.CASH
            )
        )
    }
}
