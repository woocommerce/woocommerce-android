package com.woocommerce.android.ui.woopos.home.products

sealed class WooPosProductsUIEvent {
    data class ItemClicked(val item: WooPosProductsListItem) : WooPosProductsUIEvent()
    data object EndOfProductListReached : WooPosProductsUIEvent()
    data object PullToRefreshTriggered : WooPosProductsUIEvent()
    data object SimpleProductsBannerClosed : WooPosProductsUIEvent()
    data object SimpleProductsBannerLearnMoreClicked : WooPosProductsUIEvent()
    data object SimpleProductsDialogInfoIconClicked : WooPosProductsUIEvent()
}
