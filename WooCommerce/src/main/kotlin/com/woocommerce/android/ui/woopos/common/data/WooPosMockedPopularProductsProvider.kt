package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosMockedPopularProductsProvider @Inject constructor(
    private val productsCache: WooPosProductsCache
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 10
    }

    suspend fun getPopularProducts(): List<Product> {
        val products = productsCache.getAll()
        return products.shuffled().take(minOf(products.size, MAX_POPULAR_PRODUCTS))
    }
}
