package com.woocommerce.android.ui.woopos.cartcheckout

sealed class WooPosCartCheckoutState {
    data object Cart : WooPosCartCheckoutState()
    data object Checkout : WooPosCartCheckoutState()
}
