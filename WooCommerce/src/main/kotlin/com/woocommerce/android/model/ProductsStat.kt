package com.woocommerce.android.model

data class ProductsStat(
    val itemsSold: Int,
    val itemsSoldDelta: Int,
    val products: List<ProductItem>
)

data class ProductItem(
    private val name: String,
    private val description: String,
    private val image: String,
    private val quantity: Int,
)
