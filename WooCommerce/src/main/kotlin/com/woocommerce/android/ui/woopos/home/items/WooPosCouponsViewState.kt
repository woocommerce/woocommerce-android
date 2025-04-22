package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosCouponsViewState(
    override val pullToRefreshState: WooPosPullToRefreshState,
) : WooPosBaseViewState(pullToRefreshState) {
    data class Content(
        val coupons: List<WooPosItemSelectionViewState>,
        val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)
    data class Loading(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)

    data class Error(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)
}
