package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

interface WooPosProductsCache {
    companion object {
        const val MAX_CACHE_SIZE = 2_000
    }

    suspend fun addAll(products: List<WooPosProductModel>)

    suspend fun getAll(): List<WooPosProductModel>

    suspend fun getProductById(productId: Long): WooPosProductModel?

    suspend fun getProductByGlobalUniqueIdentifier(globalUniqueIdentifier: String): WooPosProductModel?

    suspend fun updateProduct(product: WooPosProductModel)

    suspend fun deleteProduct(productId: Long)

    suspend fun clear()
}
