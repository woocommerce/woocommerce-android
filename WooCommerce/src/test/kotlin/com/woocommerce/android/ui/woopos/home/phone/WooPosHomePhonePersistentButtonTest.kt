package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhonePersistentButtonTest {

    @Test
    fun `given empty cart, when getting the button state, then hidden`() {
        // GIVEN
        val cartState = WooPosCartState(body = WooPosCartState.Body.Empty)

        // WHEN
        val result = phonePersistentButtonState(cartState, isSheetOpen = false)

        // THEN
        assertThat(result).isEqualTo(WooPosHomePhonePersistentButtonState.Hidden)
    }

    @Test
    fun `given cart with items and closed sheet, when getting the button state, then open cart with the count`() {
        // GIVEN
        val cartState = cartWithItems(3)

        // WHEN
        val result = phonePersistentButtonState(cartState, isSheetOpen = false)

        // THEN
        assertThat(result).isEqualTo(WooPosHomePhonePersistentButtonState.OpenCart(itemCount = 3))
    }

    @Test
    fun `given cart with items and open sheet, when getting the button state, then checkout`() {
        // GIVEN
        val cartState = cartWithItems(3)

        // WHEN
        val result = phonePersistentButtonState(cartState, isSheetOpen = true)

        // THEN
        assertThat(result).isEqualTo(WooPosHomePhonePersistentButtonState.Checkout(isEnabled = true))
    }

    private fun cartWithItems(count: Int) = WooPosCartState(
        body = WooPosCartState.Body.WithItems(
            itemsInCart = List(count) {
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
}
