package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemsUIEvent {
    data class ItemClicked(val item: WooPosItem) : WooPosItemsUIEvent()
    data object EndOfItemsListReached : WooPosItemsUIEvent()
    data object PullToRefreshTriggered : WooPosItemsUIEvent()
    data object ProductsLoadingErrorRetryButtonClicked : WooPosItemsUIEvent()
    data object SimpleProductsBannerClosed : WooPosItemsUIEvent()
    data object SimpleProductsBannerLearnMoreClicked : WooPosItemsUIEvent()
    data object SimpleProductsDialogInfoIconClicked : WooPosItemsUIEvent()
    data object BackButtonClicked : WooPosItemsUIEvent()
}
