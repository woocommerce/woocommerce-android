package com.woocommerce.android.ui.woopos.cartcheckout

sealed class WooPosCartCheckoutUIEvent {
    data object CheckoutClicked : WooPosCartCheckoutUIEvent()
    data object BackFromCheckoytToCartClicked : WooPosCartCheckoutUIEvent()
}
