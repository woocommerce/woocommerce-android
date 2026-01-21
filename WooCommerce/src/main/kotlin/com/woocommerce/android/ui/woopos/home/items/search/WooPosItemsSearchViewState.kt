package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.WooPosContentViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

sealed class WooPosItemsSearchViewState(
    open val pullToRefreshState: WooPosPullToRefreshState,
) {
    data class EmptySearchQuery(
        val popularItems: List<WooPosItemSelectionViewState.Product>,
        val recentSearches: List<String>,
    ) : WooPosItemsSearchViewState(WooPosPullToRefreshState.Disabled)

    data class Content(
        val searchQuery: String,
        override val items: List<WooPosItemSelectionViewState>,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) : WooPosItemsSearchViewState(pullToRefreshState), WooPosContentViewState

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosItemsSearchViewState(pullToRefreshState)
    data class Error(val searchQuery: String) : WooPosItemsSearchViewState(WooPosPullToRefreshState.Disabled)
    data object Loading : WooPosItemsSearchViewState(WooPosPullToRefreshState.Disabled)
}
