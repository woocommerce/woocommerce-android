package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.WooPosItem

sealed class WooPosItemsSearchViewState {
    data class EmptySearchQuery(
        val popularItems: List<WooPosItem.Product>,
        val recentSearches: List<String>,
    ) : WooPosItemsSearchViewState()

    object Content : WooPosItemsSearchViewState()
    data object Empty : WooPosItemsSearchViewState()
}
