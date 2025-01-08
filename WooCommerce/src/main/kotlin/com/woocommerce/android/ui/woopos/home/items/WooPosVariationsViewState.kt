package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosVariationsViewState(
    override val reloadingProductsWithPullToRefresh: Boolean
) : WooPosBaseViewState(reloadingProductsWithPullToRefresh) {

    data class Content(
        override val items: List<WooPosItem.Variation>,
        override val reloadingProductsWithPullToRefresh: Boolean = false,
        override val paginationState: PaginationState = PaginationState.None,
    ) : WooPosVariationsViewState(reloadingProductsWithPullToRefresh), ContentViewState

    data class Loading(
        override val reloadingProductsWithPullToRefresh: Boolean = false,
        val withCart: Boolean
    ) : WooPosVariationsViewState(reloadingProductsWithPullToRefresh)

    data class Error(
        override val reloadingProductsWithPullToRefresh: Boolean = false
    ) : WooPosVariationsViewState(reloadingProductsWithPullToRefresh)

    data class Empty(
        override val reloadingProductsWithPullToRefresh: Boolean = false
    ) : WooPosVariationsViewState(reloadingProductsWithPullToRefresh)
}
