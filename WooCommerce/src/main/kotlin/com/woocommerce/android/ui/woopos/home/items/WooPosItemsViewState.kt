package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class WooPosItemsViewState(
    override val reloadingProductsWithPullToRefresh: Boolean,
) : WooPosBaseViewState(reloadingProductsWithPullToRefresh) {
    data class Content(
        override val items: List<WooPosItem>,
        val bannerState: BannerState,
        override val paginationState: PaginationState = PaginationState.None,
        override val reloadingProductsWithPullToRefresh: Boolean = false
    ) : WooPosItemsViewState(reloadingProductsWithPullToRefresh), ContentViewState {
        data class BannerState(
            val isBannerHiddenByUser: Boolean,
            @StringRes val title: Int,
            @StringRes val message: Int,
            @DrawableRes val icon: Int
        )
    }

    data class Loading(
        override val reloadingProductsWithPullToRefresh: Boolean = false,
        val withCart: Boolean
    ) : WooPosItemsViewState(reloadingProductsWithPullToRefresh)

    data class Error(
        override val reloadingProductsWithPullToRefresh: Boolean = false
    ) : WooPosItemsViewState(reloadingProductsWithPullToRefresh)

    data class Empty(
        override val reloadingProductsWithPullToRefresh: Boolean = false
    ) : WooPosItemsViewState(reloadingProductsWithPullToRefresh)
}
