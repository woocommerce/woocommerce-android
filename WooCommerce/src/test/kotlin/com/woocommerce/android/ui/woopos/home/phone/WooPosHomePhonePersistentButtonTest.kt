package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhonePersistentButtonTest {

    private val resolver = WooPosPhonePersistentButtonStateResolver(
        buildCartLabel = { "Cart ($it)" },
        checkoutLabel = "Check out",
    )

    // region Products screen

    @Test
    fun `given Products screen and empty cart, when resolving state, then Hidden`() {
        val cartState = WooPosCartState(body = WooPosCartState.Body.Empty)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(WooPosPhonePersistentButtonState.Hidden)
    }

    @Test
    fun `given Products screen and cart with Enabled checkout, when resolving state, then Primary OpenCart ENABLED`() {
        val cartState = cartWithItems(checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.ENABLED,
                action = WooPosPhonePersistentButtonAction.OpenCart,
            )
        )
    }

    @Test
    fun `given Products screen and cart with Success checkout, when resolving state, then Primary OpenCart SUCCESS`() {
        val cartState = cartWithItems(checkoutButtonState = WooPosCartState.CheckoutButtonState.Success)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.SUCCESS,
                action = WooPosPhonePersistentButtonAction.OpenCart,
            )
        )
    }

    @Test
    fun `given Products screen and cart with Disabled checkout, when resolving state, then Primary OpenCart DISABLED`() {
        val cartState = cartWithItems(checkoutButtonState = WooPosCartState.CheckoutButtonState.Disabled)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.DISABLED,
                action = WooPosPhonePersistentButtonAction.OpenCart,
            )
        )
    }

    @Test
    fun `given Products screen and cart with Invisible checkout, when resolving state, then Primary OpenCart ENABLED`() {
        // Invisible maps to null via toWooPosButtonState(), falling back to ENABLED
        val cartState = cartWithItems(checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Products,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Cart (3)",
                buttonState = WooPosButtonState.ENABLED,
                action = WooPosPhonePersistentButtonAction.OpenCart,
            )
        )
    }

    // endregion

    // region Cart screen

    @Test
    fun `given Cart screen with Enabled checkout, when resolving state, then Primary Checkout ENABLED`() {
        val cartState = WooPosCartState(checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.ENABLED,
                action = WooPosPhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Cart screen with Success checkout, when resolving state, then Primary Checkout SUCCESS`() {
        val cartState = WooPosCartState(checkoutButtonState = WooPosCartState.CheckoutButtonState.Success)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.SUCCESS,
                action = WooPosPhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Cart screen with Disabled checkout, when resolving state, then Primary Checkout DISABLED`() {
        val cartState = WooPosCartState(checkoutButtonState = WooPosCartState.CheckoutButtonState.Disabled)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(
            WooPosPhonePersistentButtonState.Primary(
                label = "Check out",
                buttonState = WooPosButtonState.DISABLED,
                action = WooPosPhonePersistentButtonAction.Checkout,
            )
        )
    }

    @Test
    fun `given Cart screen with Invisible checkout, when resolving state, then Hidden`() {
        val cartState = WooPosCartState(checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible)

        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Cart,
            cartState = cartState,
        )

        assertThat(result).isEqualTo(WooPosPhonePersistentButtonState.Hidden)
    }

    // endregion

    // region Checkout screen

    @Test
    fun `given Checkout screen, when resolving state, then Hidden`() {
        val result = resolver.resolve(
            screenPositionState = WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals,
            cartState = null,
        )

        assertThat(result).isEqualTo(WooPosPhonePersistentButtonState.Hidden)
    }

    // endregion

    private fun cartWithItems(
        itemCount: Int = 3,
        checkoutButtonState: WooPosCartState.CheckoutButtonState = WooPosCartState.CheckoutButtonState.Enabled,
    ) = WooPosCartState(
        body = WooPosCartState.Body.WithItems(
            itemsInCart = List(itemCount) {
                WooPosCartItemViewState.Product.Simple(
                    itemNumber = it,
                    id = it.toLong(),
                    name = "n",
                    price = "",
                    description = null,
                    imageUrl = null,
                )
            }
        ),
        checkoutButtonState = checkoutButtonState,
    )
}
