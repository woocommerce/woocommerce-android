package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosOrdersInMemoryCache @Inject constructor() {
    private val ordersCache = AtomicReference<List<Order>>(emptyList())

    fun setAll(orders: List<Order>) {
        ordersCache.set(orders.toList())
    }

    fun getAll(): List<Order> = ordersCache.get()

    fun clear() {
        ordersCache.set(emptyList())
    }
}
