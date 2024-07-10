package com.woocommerce.android.ui.woopos.home

sealed class WooPosHomeState {
    sealed class Cart : WooPosHomeState() {
        data object Empty : Cart()
        data object NotEmpty : Cart()
    }

    sealed class Checkout : WooPosHomeState() {
        data object NotPaid : Checkout()
        data object Paid : Checkout()
    }
}
