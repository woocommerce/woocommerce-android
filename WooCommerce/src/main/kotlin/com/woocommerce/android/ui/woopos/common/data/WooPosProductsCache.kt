package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product

interface WooPosProductsCache {
    suspend fun addAll(products: List<Product>)

    suspend fun getAll(): List<Product>

    suspend fun getProductById(productId: Long): Product?

    suspend fun clear()
}
