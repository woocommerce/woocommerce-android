package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order

data class WooPosOrdersState(
    val orders: List<Order> = emptyList(),
    val selectedOrderId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val selectedOrder: Order?
        get() = selectedOrderId?.let { id -> orders.firstOrNull { it.id == id } }
}
