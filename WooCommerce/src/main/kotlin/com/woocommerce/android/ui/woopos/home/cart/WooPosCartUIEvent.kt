package com.woocommerce.android.ui.woopos.home.cart

sealed class WooPosCartUIEvent {
    data object CheckoutClicked : WooPosCartUIEvent()
    data object BackFromCheckoutToCartClicked : WooPosCartUIEvent()
    data class ItemRemovedFromCart(val item: WooPosCartListItem) : WooPosCartUIEvent()
}
