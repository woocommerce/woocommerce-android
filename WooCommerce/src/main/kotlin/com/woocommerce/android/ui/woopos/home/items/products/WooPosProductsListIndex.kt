package com.woocommerce.android.ui.woopos.home.items.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsListIndex @Inject constructor(
    private val productsCache: WooPosProductsCache
) {
    private val mutex = Mutex()

    private var productListIds: List<Long> = emptyList()

    suspend fun storeProductList(productIds: List<Long>) = mutex.withLock {
        productListIds = (productListIds + productIds).distinct()
    }

    suspend fun getProductList(): List<Product> = mutex.withLock {
        return productListIds.mapNotNull { productsCache.getProductById(it) }
    }

    suspend fun hasProducts(): Boolean = mutex.withLock {
        return productListIds.isNotEmpty()
    }

    suspend fun clearCache() = mutex.withLock {
        productListIds = emptyList()
    }
}
