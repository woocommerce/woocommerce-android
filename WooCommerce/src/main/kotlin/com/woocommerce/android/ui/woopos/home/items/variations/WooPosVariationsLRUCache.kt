package com.woocommerce.android.ui.woopos.home.items.variations

import android.util.LruCache
import com.woocommerce.android.model.ProductVariation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosVariationsLRUCache @Inject constructor() {
    companion object {
        private const val VARIATION_CACHE_MAX_SIZE = 50
    }

    private val cache = LruCache<Long, List<ProductVariation>>(VARIATION_CACHE_MAX_SIZE)
    private val mutex = Mutex()

    suspend fun get(key: Long): List<ProductVariation>? {
        return mutex.withLock {
            cache.get(key)
        }
    }

    suspend fun put(key: Long, value: List<ProductVariation>) {
        mutex.withLock {
            cache.put(key, value)
        }
    }

    suspend fun add(key: Long, value: ProductVariation) {
        mutex.withLock {
            val list = cache.get(key)
            if (list != null) {
                val updatedList = list.toMutableList().apply {
                    removeAll { it.remoteVariationId == value.remoteVariationId }
                    add(value)
                }
                cache.put(key, updatedList)
            } else {
                cache.put(key, listOf(value))
            }
        }
    }

    suspend fun getAll(): List<ProductVariation> {
        return mutex.withLock {
            cache.snapshot().values.flatten()
        }
    }
}
