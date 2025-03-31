package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.ContentViewState
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

sealed class WooPosItemsSearchViewState {
    data class EmptySearchQuery(
        val popularItems: List<WooPosItemSelectionViewState.Product>,
        val recentSearches: List<String>,
    ) : WooPosItemsSearchViewState()

    data class Content(
        val searchQuery: String,
        override val items: List<WooPosItemSelectionViewState>,
        override val reloadingWithPullToRefresh: Boolean = false,
        override val paginationState: PaginationState = PaginationState.None,
    ) : WooPosItemsSearchViewState(), ContentViewState

    data object Empty : WooPosItemsSearchViewState()
    data object Error : WooPosItemsSearchViewState()
    data object Loading : WooPosItemsSearchViewState()
}
