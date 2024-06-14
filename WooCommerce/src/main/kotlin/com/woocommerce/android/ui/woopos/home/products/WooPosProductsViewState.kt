package com.woocommerce.android.ui.woopos.home.products

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class WooPosProductsViewState(
    val products: List<WooPosProductsListItem>,
)

@Parcelize
data class WooPosProductsListItem(
    val productId: Long,
    val title: String,
    val imageUrl: String? = null,
) : Parcelable
