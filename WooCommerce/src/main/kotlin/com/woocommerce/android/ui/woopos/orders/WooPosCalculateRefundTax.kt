package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosCalculateRefundTax @Inject constructor() {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order
    ): BigDecimal {
        return refundableItems
            .groupBy { it.orderItemId }
            .entries
            .sumOf { (orderItemId, items) ->
                calculateTotalTaxesForItem(orderItemId, items.size, order)
            }
    }

    private fun calculateTotalTaxesForItem(
        orderItemId: Long,
        refundQuantity: Int,
        order: Order
    ): BigDecimal {
        val originalItem = order.items.find { it.itemId == orderItemId }

        if (originalItem == null || originalItem.quantity == 0f) {
            return BigDecimal.ZERO
        }

        return if (refundQuantity.toBigDecimal().compareTo(originalItem.quantity.toBigDecimal()) == 0) {
            originalItem.totalTax
        } else {
            val singleItemTax = originalItem.totalTax.divide(
                originalItem.quantity.toBigDecimal(),
                2,
                RoundingMode.HALF_UP
            )
            refundQuantity.toBigDecimal().multiply(singleItemTax)
        }
    }
}
