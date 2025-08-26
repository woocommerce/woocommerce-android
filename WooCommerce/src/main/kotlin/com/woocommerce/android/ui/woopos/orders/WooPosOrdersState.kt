package com.woocommerce.android.ui.woopos.orders

data class WooPosOrder(
    val id: Long,
    val title: String
)

@Suppress("MagicNumber")
data class WooPosOrdersState(
    val orders: List<WooPosOrder> = listOf(
        WooPosOrder(1, "Order 1"),
        WooPosOrder(2, "Order 2"),
        WooPosOrder(3, "Order 3")
    ),
    val selectedOrderId: Long? = 1
)
