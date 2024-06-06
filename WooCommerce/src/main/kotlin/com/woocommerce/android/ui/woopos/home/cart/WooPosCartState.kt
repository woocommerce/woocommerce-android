package com.woocommerce.android.ui.woopos.home.cart

sealed class WooPosCartState {
    data object Cart : WooPosCartState()
    data object Checkout : WooPosCartState()
}
