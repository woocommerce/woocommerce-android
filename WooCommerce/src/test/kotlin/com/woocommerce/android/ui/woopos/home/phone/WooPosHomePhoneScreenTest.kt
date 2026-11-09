package com.woocommerce.android.ui.woopos.home.phone

import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomePhoneScreenTest {

    @Test
    fun `given Products position, when mapping screen, then Products`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Products)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Products)
    }

    @Test
    fun `given Cart position, when mapping screen, then Cart`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Cart)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Cart)
    }

    @Test
    fun `given Checkout position, when mapping screen, then Checkout`() {
        // WHEN
        val result = phoneScreenFor(ScreenPositionState.Checkout.CartWithTotals)

        // THEN
        assertThat(result).isEqualTo(WooPosPhoneScreen.Checkout)
    }
}
