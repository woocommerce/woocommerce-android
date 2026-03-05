package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import javax.inject.Inject
import kotlin.math.abs

class WooPosGetNonRefundedItems @Inject constructor() {
    operator fun invoke(
        order: Order,
        refunds: List<Refund>,
    ): List<Order.Item> {
        if (refunds.isEmpty()) return order.items

        val refundedByItemId = refunds
            .flatMap { it.items }
            .groupingBy { it.orderItemId }
            .fold(0) { acc, item -> acc + item.quantity }

        return order.items.mapNotNull { item ->
            val refundedQty = abs(refundedByItemId[item.itemId] ?: 0).toFloat()
            val remaining = item.quantity - refundedQty
            when {
                remaining <= 0f -> null
                remaining == item.quantity -> item
                else -> {
                    val newTotal = item.total * (remaining / item.quantity).toBigDecimal()
                    item.copy(quantity = remaining, total = newTotal)
                }
            }
        }
    }
}
