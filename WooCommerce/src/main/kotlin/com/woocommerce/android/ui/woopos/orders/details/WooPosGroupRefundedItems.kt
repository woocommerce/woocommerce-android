package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Refund
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosGroupRefundedItems @Inject constructor() {
    operator fun invoke(refunds: List<Refund>): List<Refund.Item> {
        val allRefundItems = refunds.flatMap { it.items }
        if (allRefundItems.isEmpty()) return emptyList()

        return allRefundItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                val quantity = items.sumOf { it.quantity }
                val total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total }
                val totalTax = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.totalTax }
                items.first().copy(
                    orderItemId = orderItemId,
                    quantity = quantity,
                    total = total,
                    totalTax = totalTax,
                    price = if (quantity != 0) {
                        total.divide(BigDecimal.valueOf(quantity.toLong()), total.scale(), RoundingMode.HALF_UP)
                    } else {
                        total
                    },
                )
            }
    }
}
