package com.woocommerce.android.ui.woopos.home.products

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class WooPosProductsViewState(
    open val reloadingProductsWithPullToRefresh: Boolean,
) {
    data class Content(
        val products: List<WooPosProductsListItem>,
        val loadingMore: Boolean,
        val bannerState: BannerState,
        override val reloadingProductsWithPullToRefresh: Boolean = false,
    ) : WooPosProductsViewState(reloadingProductsWithPullToRefresh) {
        data class BannerState(
            val isBannerHiddenByUser: Boolean,
            @StringRes val title: Int,
            @StringRes val message: Int,
            @DrawableRes val icon: Int,
        )
    }

    data class Loading(
        override val reloadingProductsWithPullToRefresh: Boolean = false,
        val withCart: Boolean,
    ) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data class Error(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data class Empty(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)
}

data class WooPosProductsListItem(
    val id: Long,
    val name: String,
    val price: String,
    val imageUrl: String?,
)
