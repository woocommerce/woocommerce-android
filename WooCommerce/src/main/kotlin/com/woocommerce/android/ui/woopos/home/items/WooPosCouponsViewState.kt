package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosCouponsViewState(
    override val pullToRefreshState: WooPosPullToRefreshState,
) : WooPosBaseViewState(pullToRefreshState) {
    data class Content(
        override val items: List<WooPosItemSelectionViewState>,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState), WooPosContentViewState

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

fun WooPosCouponsViewState.updatePullToRefreshState(newState: WooPosPullToRefreshState): WooPosCouponsViewState =
    when (this) {
        is WooPosCouponsViewState.Content -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Loading -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Error -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Empty -> this.copy(pullToRefreshState = newState)
    }
