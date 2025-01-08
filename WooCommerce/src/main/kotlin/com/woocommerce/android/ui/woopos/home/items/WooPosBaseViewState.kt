package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosBaseViewState(
    open val reloadingProductsWithPullToRefresh: Boolean
)

interface ContentViewState {
    val items: List<WooPosItem>
    val reloadingProductsWithPullToRefresh: Boolean
    val paginationState: PaginationState
}

sealed class PaginationState {
    data object None : PaginationState()
    data object Loading : PaginationState()
    data object Error : PaginationState()
}
