package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosProductsViewState(
    override val pullToRefreshState: WooPosPullToRefreshState,
) : WooPosBaseViewState(pullToRefreshState) {
    data class Content(
        override val items: List<WooPosItemSelectionViewState>,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosProductsViewState(pullToRefreshState), WooPosContentViewState

    data class Loading(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosProductsViewState(pullToRefreshState)

    data class Error(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosProductsViewState(pullToRefreshState)

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosProductsViewState(pullToRefreshState)
}
