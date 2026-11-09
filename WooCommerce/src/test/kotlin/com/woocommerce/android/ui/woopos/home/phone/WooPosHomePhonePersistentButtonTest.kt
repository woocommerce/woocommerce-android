package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhonePersistentButtonTest {

    @Test
    fun `given Products screen and empty cart, when resolving state, then Hidden`() {
        // GIVEN
        val cartState = WooPosCartState(body = WooPosCartState.Body.Empty)
        val resolver = WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { "Cart ($it)" },
            checkoutLabel = "Check out",
        )

        // WHEN
        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        // THEN
        assertThat(result).isEqualTo(WooPosPhonePersistentButtonState.Hidden)
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
        val resolver = WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { "Cart ($it)" },
            checkoutLabel = "Check out",
        )

        // WHEN
        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        // THEN
        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.ENABLED,
                action = WooPosPhonePersistentButtonAction.OpenCart,
            )
        )
    }

    @Test
    fun `given Cart screen with Enabled checkout, when resolving state, then Primary Checkout ENABLED`() {
        // GIVEN
        val cartState = WooPosCartState(
            checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled
        )
        val resolver = WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { "Cart ($it)" },
            checkoutLabel = "Check out",
        )

        // WHEN
        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
        )

        // THEN
        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.ENABLED,
                action = WooPosPhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Checkout screen, when resolving state, then Hidden`() {
        // GIVEN
        val resolver = WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { "Cart ($it)" },
            checkoutLabel = "Check out",
        )

        // WHEN
        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals,
            cartState = null,
        )

        // THEN
        assertThat(result).isEqualTo(WooPosPhonePersistentButtonState.Hidden)
    }
}
