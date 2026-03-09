package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import java.math.RoundingMode
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
            .fold(0) { acc, item -> acc + abs(item.quantity) }

        return order.items.mapNotNull { item ->
            val refundedQty = (refundedByItemId[item.itemId] ?: 0).toFloat()

            if (item.quantity == 0f) {
                return@mapNotNull if (refundedQty == 0f) item else null
            }

            val remaining = item.quantity - refundedQty
            when {
                remaining <= 0f -> null
                remaining == item.quantity -> item
                else -> {
                    val newTotal = (item.total * remaining.toBigDecimal()).divide(
                        item.quantity.toBigDecimal(),
                        item.total.scale(),
                        RoundingMode.HALF_UP
                    )
                    item.copy(quantity = remaining, total = newTotal)
                }
            }
        }
    }
}
