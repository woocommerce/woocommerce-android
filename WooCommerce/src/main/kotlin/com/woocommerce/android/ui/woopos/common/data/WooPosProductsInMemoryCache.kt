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

    override suspend fun addProducts(products: List<Product>) = mutex.withLock {
        products.forEach { product ->
            productsCache[product.remoteId] = product
        }
    }

    override suspend fun getProducts(): List<Product> = mutex.withLock {
        return productsCache.values.toList()
    }

    override suspend fun getProductById(productId: Long): Product? = mutex.withLock {
        return productsCache[productId]
    }

    override suspend fun searchLocally(query: String): List<Product> = mutex.withLock {
        if (query.isBlank()) return emptyList()

        val searchTerms = query.lowercase().split(" ").filter { it.isNotBlank() }
        return productsCache.values.filter { product ->
            searchTerms.all { term ->
                product.name.lowercase().contains(term) ||
                    product.description.lowercase().contains(term) == true ||
                    product.shortDescription.lowercase().contains(term) == true
            }
        }
    }

    override suspend fun clearCache() = mutex.withLock {
        productsCache.clear()
    }
}
