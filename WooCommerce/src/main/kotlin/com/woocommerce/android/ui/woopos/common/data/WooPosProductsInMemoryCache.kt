package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Product
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsInMemoryCache @Inject constructor() : WooPosProductsCache {
    private val mutex = Mutex()

    companion object {
        private const val MAX_CACHE_SIZE = 2_000
        private const val INITIAL_CAPACITY = 200
        private const val LOAD_FACTOR = 0.75f
    }

    private val productsCache = LinkedHashMap<Long, Product>(INITIAL_CAPACITY, LOAD_FACTOR, true)

    override suspend fun addAll(products: List<Product>) = mutex.withLock {
        products.forEach { product ->
            productsCache[product.remoteId] = product
            if (productsCache.size > MAX_CACHE_SIZE) {
                val keysToRemove = productsCache.keys.take(productsCache.size - MAX_CACHE_SIZE)
                keysToRemove.forEach { productsCache.remove(it) }
            }
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
