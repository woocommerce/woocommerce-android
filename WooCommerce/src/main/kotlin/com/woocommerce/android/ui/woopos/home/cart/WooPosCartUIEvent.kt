package com.woocommerce.android.ui.woopos.home.cart

sealed class WooPosCartUIEvent {
    data object CheckoutClicked : WooPosCartUIEvent()
    data class ItemRemovedFromCart(val item: WooPosCartState.Body.WithItems.Item) : WooPosCartUIEvent()
    data object ClearAllClicked : WooPosCartUIEvent()
    data object BackClicked : WooPosCartUIEvent()
    data class OnCartItemAppearanceAnimationPlayed(val item: WooPosCartState.Body.WithItems.Item) : WooPosCartUIEvent()
}
