package com.woocommerce.android.ui.woopos.home.products

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class WooPosProductsViewState(
    val products: List<WooPosProductsListItem> = emptyList()
)

@Parcelize
data class WooPosProductsListItem(
    val id: Long,
    val name: String,
    val price: String,
    val imageUrl: String?,
) : Parcelable
