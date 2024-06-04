package com.woocommerce.android.ui.woopos.cartcheckout

sealed class WooPosCartCheckoutUIEvent {
    data object CheckoutClicked : WooPosCartCheckoutUIEvent()
    data object BackFromCheckoutToCartClicked : WooPosCartCheckoutUIEvent()
    data object SystemBackClicked : WooPosCartCheckoutUIEvent()
}
