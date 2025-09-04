package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Order

interface WooPosOrdersCache {
    companion object {
        const val MAX_CACHE_SIZE = 25
    }

    suspend fun addAll(orders: List<Order>)

    suspend fun getAll(): List<Order>

    suspend fun clear()
}
