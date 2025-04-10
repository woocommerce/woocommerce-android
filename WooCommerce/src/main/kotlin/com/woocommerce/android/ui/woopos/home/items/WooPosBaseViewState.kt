package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosBaseViewState(
    open val pullToRefreshState: WooPosPullToRefreshState
)

interface WooPosContentViewState {
    val items: List<WooPosItemSelectionViewState>
    val pullToRefreshState: WooPosPullToRefreshState
    val paginationState: WooPosPaginationState
}

sealed class WooPosPaginationState {
    data object None : WooPosPaginationState()
    data object Loading : WooPosPaginationState()
    data object Error : WooPosPaginationState()
}

enum class WooPosPullToRefreshState {
    Disabled, Enabled, Refreshing,
}
