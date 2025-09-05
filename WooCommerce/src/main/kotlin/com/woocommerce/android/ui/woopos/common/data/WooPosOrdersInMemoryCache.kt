package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource.Companion.POS_ORDERS_PAGE_SIZE
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class WooPosOrdersInMemoryCache @Inject constructor() {
    private val mutex = Mutex()

    companion object {
        private const val CACHE_CAPACITY = POS_ORDERS_PAGE_SIZE
        private const val LOAD_FACTOR = 0.75f
    }

    private val ordersCache = LinkedHashMap<Long, Order>(CACHE_CAPACITY, LOAD_FACTOR, true)

    suspend fun addAll(orders: List<Order>) = mutex.withLock {
        addAllInternal(orders)
    }

    suspend fun getAll(): List<Order> = mutex.withLock {
        ordersCache.values.toList()
    }

    suspend fun clear() = mutex.withLock {
        ordersCache.clear()
    }

    private fun addAllInternal(orders: List<Order>) {
        orders.forEach { order ->
            ordersCache[order.id] = order
            if (ordersCache.size > CACHE_CAPACITY) {
                val keysToRemove = ordersCache.keys.take(ordersCache.size - CACHE_CAPACITY)
                keysToRemove.forEach { ordersCache.remove(it) }
            }
        }
    }
}
