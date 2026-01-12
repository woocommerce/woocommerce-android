package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

class WooPosCalculateRefundTax @Inject constructor() {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order,
        numberOfDecimals: Int,
    ): BigDecimal {
        return refundableItems
            .groupBy { it.orderItemId }
            .entries
            .sumOf { (orderItemId, items) ->
                calculateTotalTaxesForItem(orderItemId, items.size, order, numberOfDecimals)
            }
    }

    private fun calculateTotalTaxesForItem(
        orderItemId: Long,
        refundQuantity: Int,
        order: Order,
        numberOfDecimals: Int
    ): BigDecimal {
        val originalItem = requireNotNull(order.items.find { it.itemId == orderItemId }) {
            "Order item with ID $orderItemId not found in order ${order.id}."
        }

        check(originalItem.quantity > 0f) {
            "Order item $orderItemId has invalid quantity ${originalItem.quantity}."
        }

        return if (refundQuantity.toBigDecimal().compareTo(originalItem.quantity.toBigDecimal()) == 0) {
            originalItem.totalTax
        } else {
            // Calculate per-unit tax with high precision to preserve accuracy
            val perUnitTax = originalItem.totalTax.divide(
                originalItem.quantity.toBigDecimal(),
                MathContext.DECIMAL128
            )

            refundQuantity.toBigDecimal().multiply(perUnitTax)
        }
    }
}
