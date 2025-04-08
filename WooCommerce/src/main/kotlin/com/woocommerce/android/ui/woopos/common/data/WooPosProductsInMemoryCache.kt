package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsInMemoryCache @Inject constructor() : WooPosProductsCache {
    private val mutex = Mutex()

    private val productsCache = mutableMapOf<Long, Product>()

    override suspend fun addAll(products: List<Product>) = mutex.withLock {
        products.forEach { product ->
            productsCache[product.remoteId] = product
        }
    }

    override suspend fun getAll(): List<Product> = mutex.withLock {
        return productsCache.values.toList()
    }

    override suspend fun getProductById(productId: Long): Product? = mutex.withLock {
        return productsCache[productId]
    }

    override suspend fun clear() = mutex.withLock {
        productsCache.clear()
    }
}
