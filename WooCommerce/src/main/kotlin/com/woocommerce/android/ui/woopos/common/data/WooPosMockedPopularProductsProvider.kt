package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsIndex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosMockedPopularProductsProvider @Inject constructor(
    private val productsIndex: WooPosProductsIndex,
) {
    companion object {
        private const val MAX_POPULAR_PRODUCTS = 10
    }

    suspend fun getPopularProducts(): List<Product> {
        val products = productsIndex.getProductList()
        return products.shuffled().take(minOf(products.size, MAX_POPULAR_PRODUCTS))
    }
}
