package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosVariationsViewState(
    override val reloadingWithPullToRefresh: Boolean
) : WooPosBaseViewState(reloadingWithPullToRefresh) {

    data class Content(
        override val items: List<WooPosItemSelectionViewState.Variation>,
        override val reloadingWithPullToRefresh: Boolean = false,
        override val paginationState: PaginationState = PaginationState.None,
    ) : WooPosVariationsViewState(reloadingWithPullToRefresh), ContentViewState

    data class Loading(
        override val reloadingWithPullToRefresh: Boolean = false,
        val withCart: Boolean
    ) : WooPosVariationsViewState(reloadingWithPullToRefresh)

    data class Error(
        override val reloadingWithPullToRefresh: Boolean = false
    ) : WooPosVariationsViewState(reloadingWithPullToRefresh)

    data class Empty(
        override val reloadingWithPullToRefresh: Boolean = false
    ) : WooPosVariationsViewState(reloadingWithPullToRefresh)
}
