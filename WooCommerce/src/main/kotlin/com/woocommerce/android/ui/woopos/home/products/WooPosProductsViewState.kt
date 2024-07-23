package com.woocommerce.android.ui.woopos.home.products

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class WooPosProductsViewState(
    open val reloadingProductsWithPullToRefresh: Boolean,
) {
    data class Content(
        val products: List<WooPosProductsListItem>,
        val loadingMore: Boolean,
        override val reloadingProductsWithPullToRefresh: Boolean = false,
    ) : WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data class Loading(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data class Error(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data class Empty(override val reloadingProductsWithPullToRefresh: Boolean = false) :
        WooPosProductsViewState(reloadingProductsWithPullToRefresh)

    data object Unknown : WooPosProductsViewState(false)
}

@Parcelize
data class WooPosProductsListItem(
    val id: Long,
    val name: String,
    val price: String,
    val imageUrl: String?,
) : Parcelable
