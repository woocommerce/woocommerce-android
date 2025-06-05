package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product

interface WooPosProductsCache {
    companion object {
        const val MAX_CACHE_SIZE = 2_000
    }

    suspend fun addAll(products: List<Product>)

    suspend fun getAll(): List<Product>

    suspend fun getProductById(productId: Long): Product?

    suspend fun getProductByGtin(gtin: String): Product?

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(productId: Long)

    suspend fun clear()
}
