package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosOrdersCache
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosOrdersCache.Companion.MAX_CACHE_SIZE
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class WooPosOrdersInMemoryCache @Inject constructor() : WooPosOrdersCache {
    private val mutex = Mutex()

    companion object {
        private const val INITIAL_CAPACITY = MAX_CACHE_SIZE
        private const val LOAD_FACTOR = 0.75f
    }

    private val ordersCache = LinkedHashMap<Long, Order>(INITIAL_CAPACITY, LOAD_FACTOR, true)

    override suspend fun addAll(orders: List<Order>) = mutex.withLock {
        addAllInternal(orders)
    }

    override suspend fun getAll(): List<Order> = mutex.withLock {
        ordersCache.values.toList()
    }

    override suspend fun clear() = mutex.withLock {
        ordersCache.clear()
    }

    private fun addAllInternal(orders: List<Order>) {
        orders.forEach { order ->
            ordersCache[order.id] = order
            if (ordersCache.size > MAX_CACHE_SIZE) {
                val keysToRemove = ordersCache.keys.take(ordersCache.size - MAX_CACHE_SIZE)
                keysToRemove.forEach { ordersCache.remove(it) }
            }
        }
    }
}
