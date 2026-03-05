package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Refund
import java.math.BigDecimal
import javax.inject.Inject

class WooPosGroupRefundedItems @Inject constructor() {
    operator fun invoke(refunds: List<Refund>): List<Refund.Item> {
        val allRefundItems = refunds.flatMap { it.items }
        if (allRefundItems.isEmpty()) return emptyList()

        return allRefundItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                items.first().copy(
                    orderItemId = orderItemId,
                    quantity = items.sumOf { it.quantity },
                    total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total },
                )
            }
    }
}
