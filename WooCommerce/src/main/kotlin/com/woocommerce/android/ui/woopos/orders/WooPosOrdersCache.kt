package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order

interface WooPosOrdersCache {
    companion object {
        const val MAX_CACHE_SIZE = 25
    }

    suspend fun addAll(orders: List<Order>)

    suspend fun getAll(): List<Order>

    suspend fun clear()
}
