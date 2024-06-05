package com.woocommerce.android.ui.woopos.home

sealed class WooPosHomeUIEvent {
    data object CheckoutClicked : WooPosHomeUIEvent()
    data object BackFromCheckoutToCartClicked : WooPosHomeUIEvent()
    data object SystemBackClicked : WooPosHomeUIEvent()
}
