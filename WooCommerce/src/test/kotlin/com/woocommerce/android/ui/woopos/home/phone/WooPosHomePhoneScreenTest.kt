package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhoneScreenTest {

    @Test
    fun `given Cart position and custom amount form closed, when mapping screen, then Cart`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Cart, isCustomAmountFormOpen = false)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Cart)
    }

    @Test
    fun `given Cart position and custom amount form open, when mapping screen, then Products`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Cart, isCustomAmountFormOpen = true)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Products)
    }

    @Test
    fun `given Products position and custom amount form open, when mapping screen, then Products`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Products, isCustomAmountFormOpen = true)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Products)
    }

    @Test
    fun `given Checkout position and custom amount form open, when mapping screen, then Checkout`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Checkout.CartWithTotals, isCustomAmountFormOpen = true)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Checkout)
    }
}
