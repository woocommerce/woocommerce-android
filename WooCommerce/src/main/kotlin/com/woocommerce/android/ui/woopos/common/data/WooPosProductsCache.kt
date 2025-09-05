package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2

interface WooPosProductsCache {
    companion object {
        const val MAX_CACHE_SIZE = 2_000
    }

    suspend fun addAll(products: List<WooPosProductModelVersion2>)

    suspend fun getAll(): List<WooPosProductModelVersion2>

    suspend fun getProductById(productId: Long): WooPosProductModelVersion2?

    suspend fun getProductByGlobalUniqueIdentifier(globalUniqueIdentifier: String): WooPosProductModelVersion2?

    suspend fun updateProduct(product: WooPosProductModelVersion2)

    suspend fun deleteProduct(productId: Long)

    suspend fun clear()
}
