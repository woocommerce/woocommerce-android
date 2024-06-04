package com.woocommerce.android.ui.woopos.cartcheckout.cart

sealed class WooPosCartState {
    data object Cart : WooPosCartState()
    data object Checkout : WooPosCartState()
}
