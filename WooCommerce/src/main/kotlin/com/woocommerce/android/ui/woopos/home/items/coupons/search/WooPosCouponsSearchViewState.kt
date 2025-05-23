package com.woocommerce.android.ui.woopos.home.items.coupons.search

import com.woocommerce.android.ui.woopos.home.items.WooPosContentViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

sealed class WooPosCouponsSearchViewState {
    data class EmptySearchQuery(
        val recentSearches: List<String>,
    ) : WooPosCouponsSearchViewState()

    data class Content(
        val searchQuery: String,
        override val items: List<WooPosItemSelectionViewState.Coupon>,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) : WooPosCouponsSearchViewState(), WooPosContentViewState

    data object Empty : WooPosCouponsSearchViewState()
    data class Error(val searchQuery: String) : WooPosCouponsSearchViewState()
    data object Loading : WooPosCouponsSearchViewState()
}
