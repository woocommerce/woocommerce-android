package com.woocommerce.android.ui.woopos.home.products

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class WooPosProductsViewState {
    data class Content(
        val products: List<WooPosProductsListItem>,
        val loadingMore: Boolean,
    ) : WooPosProductsViewState()
    data object Loading : WooPosProductsViewState()
    data object Error : WooPosProductsViewState()
    data object Empty : WooPosProductsViewState()
}

@Parcelize
data class WooPosProductsListItem(
    val id: Long,
    val name: String,
    val price: String,
    val imageUrl: String?,
) : Parcelable
