package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order

interface WooPosOrdersCache {
    suspend fun addAll(orders: List<Order>)
}
