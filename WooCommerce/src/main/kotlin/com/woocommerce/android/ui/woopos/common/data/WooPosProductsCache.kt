package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product

interface WooPosProductsCache {
    suspend fun addProducts(products: List<Product>)

    suspend fun getProducts(): List<Product>

    suspend fun getProductById(productId: Long): Product?

    suspend fun clearCache()

    suspend fun searchLocally(query: String): List<Product>
}
