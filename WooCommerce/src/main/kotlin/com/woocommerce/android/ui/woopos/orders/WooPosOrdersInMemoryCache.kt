package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class WooPosOrdersInMemoryCache @Inject constructor() : WooPosOrdersCache {
    private val mutex = Mutex()

    override suspend fun addAll(orders: List<Order>) = mutex.withLock {
    }
}
