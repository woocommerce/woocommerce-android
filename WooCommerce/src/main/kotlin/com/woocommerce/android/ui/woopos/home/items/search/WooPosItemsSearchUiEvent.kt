package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

sealed class WooPosItemsSearchUiEvent {
    data class OnItemClicked(val item: WooPosItemSelectionViewState) : WooPosItemsSearchUiEvent()
    data object OnNextPageRequested : WooPosItemsSearchUiEvent()
    data object LoadingErrorRetryButtonClicked : WooPosItemsSearchUiEvent()
    data class OnRecentSearchClicked(val recentSearch: String) : WooPosItemsSearchUiEvent()
    data class OnPopularItemClicked(val item: WooPosItemSelectionViewState) : WooPosItemsSearchUiEvent()
}
