package com.woocommerce.android.ui.woopos.home

sealed class WooPosHomeState {
    data object Cart : WooPosHomeState()

    data object Checkout : WooPosHomeState()
}
