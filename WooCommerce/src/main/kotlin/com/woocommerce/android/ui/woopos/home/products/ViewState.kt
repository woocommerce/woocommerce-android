package com.woocommerce.android.ui.woopos.home.products

data class ViewState(
    val products: List<ListItem>,
)

data class ListItem(
    val productId: Long,
    val title: String,
    val imageUrl: String? = null,
)
