package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhonePersistentButtonTest {

    @Test
    fun `given Products screen and empty cart, when resolving state, then Hidden`() {
        // GIVEN
        val cartState = WooPosCartState(body = WooPosCartState.Body.Empty)

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(PhonePersistentButtonState.Hidden)
    }

    @Test
    fun `given Products screen and WithItems with empty list, when resolving state, then Hidden`() {
        // GIVEN
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(itemsInCart = emptyList())
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(PhonePersistentButtonState.Hidden)
    }

    @Test
    fun `given Products screen and cart with items, when resolving state, then Primary OpenCart`() {
        // GIVEN
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = List(3) {
                    WooPosCartItemViewState.Product.Simple(
                        itemNumber = it,
                        id = it.toLong(),
                        name = "n",
                        price = "",
                        description = null,
                        imageUrl = null,
                    )
                }
            )
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(
            PhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.ENABLED,
                action = PhonePersistentButtonAction.OpenCart,
            )
        )
    }

    @Test
    fun `given Cart screen with Enabled checkout, when resolving state, then Primary Checkout ENABLED`() {
        // GIVEN
        val cartState = WooPosCartState(
            checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(
            PhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.ENABLED,
                action = PhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Cart screen with Disabled checkout, when resolving state, then Primary Checkout DISABLED`() {
        // GIVEN
        val cartState = WooPosCartState(
            checkoutButtonState = WooPosCartState.CheckoutButtonState.Disabled
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(
            PhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.DISABLED,
                action = PhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Cart screen with Invisible checkout, when resolving state, then Hidden`() {
        // GIVEN
        val cartState = WooPosCartState(
            checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
            totalsState = null,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(PhonePersistentButtonState.Hidden)
    }

    @Test
    fun `given Checkout screen and totals is Checkout, when resolving state, then Outlined CashPayment`() {
        // GIVEN
        val totalsState = WooPosTotalsViewState.Checkout(
            totals = WooPosTotalsViewState.Totals.Hidden,
            readerStatus = WooPosTotalsViewState.ReaderStatus.Unavailable,
        )

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals,
            cartState = null,
            totalsState = totalsState,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(
            PhonePersistentButtonState.Outlined(
                label = "Cash payment",
                action = PhonePersistentButtonAction.CashPayment,
            )
        )
    }

    @Test
    fun `given Checkout screen and totals is PaymentInProgress, when resolving state, then Hidden`() {
        // GIVEN
        val totalsState = WooPosTotalsViewState.PaymentInProgress(title = "t", subtitle = "s")

        // WHEN
        val result = resolvePhonePersistentButtonState(
            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals,
            cartState = null,
            totalsState = totalsState,
            cartLabelBuilder = { "Cart ($it)" },
            checkoutLabel = "Check out",
            cashPaymentLabel = "Cash payment",
        )

        // THEN
        assertThat(result).isEqualTo(PhonePersistentButtonState.Hidden)
    }
}
