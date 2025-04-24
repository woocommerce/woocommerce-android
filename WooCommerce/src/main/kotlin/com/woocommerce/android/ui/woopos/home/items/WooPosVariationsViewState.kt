package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosVariationsViewState(
    open val pullToRefreshState: WooPosPullToRefreshState
) {
    data class Content(
        override val items: List<WooPosItemSelectionViewState.Variation>,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) : WooPosVariationsViewState(pullToRefreshState), WooPosContentViewState

    data class Loading(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosVariationsViewState(pullToRefreshState)

    data class Error(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled,
    ) : WooPosVariationsViewState(pullToRefreshState)

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosVariationsViewState(pullToRefreshState)
}
