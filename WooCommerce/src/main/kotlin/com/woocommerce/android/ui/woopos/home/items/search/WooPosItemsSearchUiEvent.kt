package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

sealed class WooPosItemsSearchUiEvent {
    data class ItemClicked(val item: WooPosItemSelectionViewState) : WooPosItemsSearchUiEvent()
    data object OnNextPageRequested : WooPosItemsSearchUiEvent()
    data object LoadingErrorRetryButtonClicked : WooPosItemsSearchUiEvent()
}
