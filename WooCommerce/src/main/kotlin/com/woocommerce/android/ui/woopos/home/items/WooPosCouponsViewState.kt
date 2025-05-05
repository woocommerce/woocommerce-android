package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosCouponsViewState(
    open val pullToRefreshState: WooPosPullToRefreshState,
) {
    data class Content(
        override val items: List<WooPosItemSelectionViewState>,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState), WooPosContentViewState

    data class Loading(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)

    sealed class Error(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled,
    ) : WooPosCouponsViewState(pullToRefreshState) {
        data class GenericError(
            override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
        ) : Error(pullToRefreshState = pullToRefreshState)

        data class CouponsDisabledError(
            override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Disabled
        ) : Error(pullToRefreshState = pullToRefreshState)
    }

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosCouponsViewState(pullToRefreshState)
}

fun WooPosCouponsViewState.updatePullToRefreshState(newState: WooPosPullToRefreshState): WooPosCouponsViewState =
    when (this) {
        is WooPosCouponsViewState.Content -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Loading -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Error.GenericError -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Error.CouponsDisabledError -> this.copy(pullToRefreshState = newState)
        is WooPosCouponsViewState.Empty -> this.copy(pullToRefreshState = newState)
    }
