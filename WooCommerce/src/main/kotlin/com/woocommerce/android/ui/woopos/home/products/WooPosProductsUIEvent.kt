package com.woocommerce.android.ui.woopos.home.products

sealed class WooPosProductsUIEvent {
    data class ItemClicked(val item: WooPosProductsListItem) : WooPosProductsUIEvent()
    data object EndOfProductsListReached : WooPosProductsUIEvent()
    data object PullToRefreshTriggered : WooPosProductsUIEvent()
}
