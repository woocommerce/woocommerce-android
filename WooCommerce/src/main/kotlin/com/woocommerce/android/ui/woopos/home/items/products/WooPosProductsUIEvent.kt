package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

sealed class WooPosProductsUIEvent {
    data class ItemClicked(val item: WooPosItemSelectionViewState) : WooPosProductsUIEvent()
    data object EndOfItemsListReached : WooPosProductsUIEvent()
    data object PullToRefreshTriggered : WooPosProductsUIEvent()
    data object ProductsLoadingErrorRetryButtonClicked : WooPosProductsUIEvent()
    data object CustomAmountEntryRowClicked : WooPosProductsUIEvent()
}
