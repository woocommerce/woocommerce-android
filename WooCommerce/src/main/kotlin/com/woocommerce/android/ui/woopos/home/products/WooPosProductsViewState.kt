package com.woocommerce.android.ui.woopos.home.products

data class WooPosProductsViewState(
    val products: List<WooPosProductsListItem>,
)

data class WooPosProductsListItem(
    val productId: Long,
    val title: String,
    val imageUrl: String? = null,
)