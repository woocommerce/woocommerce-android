package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem

sealed class WooPosCartUIEvent {
    data object CheckoutClicked : WooPosCartUIEvent()
    data object BackFromCheckoutToCartClicked : WooPosCartUIEvent()
    data class ItemRemovedFromCart(val item: WooPosProductsListItem) : WooPosCartUIEvent()
}
