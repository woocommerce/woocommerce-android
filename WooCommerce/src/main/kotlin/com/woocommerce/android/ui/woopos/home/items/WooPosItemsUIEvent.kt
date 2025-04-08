package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemsUIEvent {
    data class ItemClicked(val item: WooPosItemSelectionViewState) : WooPosItemsUIEvent()
    data object EndOfItemsListReached : WooPosItemsUIEvent()
    data object PullToRefreshTriggered : WooPosItemsUIEvent()
    data object ProductsLoadingErrorRetryButtonClicked : WooPosItemsUIEvent()
    data object SimpleProductsBannerClosed : WooPosItemsUIEvent()
    data object SimpleProductsBannerLearnMoreClicked : WooPosItemsUIEvent()
    data object SimpleProductsDialogInfoIconClicked : WooPosItemsUIEvent()
    data object CouponsButtonClicked : WooPosItemsUIEvent()
    data object BackButtonClicked : WooPosItemsUIEvent()

    data object ClearSearchClicked : WooPosItemsUIEvent()
    data class SearchChanged(val query: String) : WooPosItemsUIEvent()
    data object CloseSearchClicked : WooPosItemsUIEvent()
    object SearchAnimationComplete : WooPosItemsUIEvent()
}
