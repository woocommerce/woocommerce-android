package com.woocommerce.android.ui.woopos.home.products

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosProductsViewState(
    open val reloadingProductsWithPullToRefresh: Boolean,
) : Parcelable {
    @Parcelize
    data class Content(
        val products: List<WooPosProductsListItem>,
        val loadingMore: Boolean,
        val bannerState: BannerState,
        override val reloadingProductsWithPullToRefresh: Boolean = false,
    ) : WooPosProductsViewState(reloadingProductsWithPullToRefresh) {
        @Parcelize
        data class BannerState(
            val isBannerHiddenByUser: Boolean,
            @StringRes val title: Int,
            @StringRes val message: Int,
            @DrawableRes val icon: Int,
        ) : Parcelable
    }

    @Parcelize
    data class Loading(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    @Parcelize
    data class Error(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    @Parcelize
    data class Empty(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)
}

@Parcelize
data class WooPosProductsListItem(
    val id: Long,
    val name: String,
    val price: String,
    val imageUrl: String?,
) : Parcelable
