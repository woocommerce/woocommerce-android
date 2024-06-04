package com.woocommerce.android.ui.woopos.cartcheckout.cart

sealed class WooPosCartUIEvent {
    data object CheckoutClicked : WooPosCartUIEvent()
    data object BackFromCheckoytToCartClicked : WooPosCartUIEvent()
}
