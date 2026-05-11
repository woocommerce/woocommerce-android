package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

class WooPosCalculateRefundTax @Inject constructor() {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order,
        numberOfDecimals: Int,
        roundAtSubtotal: Boolean
    ): BigDecimal {
        if (refundableItems.isEmpty()) {
            return BigDecimal.ZERO
        }

        val (lumpSumItems, productRows) = refundableItems.partition { it.isLumpSum }

        val productTax = productRows
            .groupBy { it.orderItemId }
            .entries
            .sumOf { (orderItemId, items) ->
                calculateTotalTaxesForItem(orderItemId, items.size, order, numberOfDecimals, roundAtSubtotal)
            }

        val lumpSumTax = lumpSumItems.sumOf { it.unitTax }

        return (productTax + lumpSumTax).setScale(numberOfDecimals, RoundingMode.HALF_UP)
    }

    private fun calculateTotalTaxesForItem(
        orderItemId: Long,
        refundQuantity: Int,
        order: Order,
        numberOfDecimals: Int,
        roundAtSubtotal: Boolean
    ): BigDecimal {
        val originalItem = requireNotNull(order.items.find { it.itemId == orderItemId }) {
            "Order item with ID $orderItemId not found in order ${order.id}."
        }

        check(originalItem.quantity > 0f) {
            "Order item $orderItemId has invalid quantity ${originalItem.quantity}."
        }

        val itemTax = if (refundQuantity.toBigDecimal().compareTo(originalItem.quantity.toBigDecimal()) == 0) {
            originalItem.taxes.sumOf { it.taxAmount }
        } else {
            // Calculate per-unit tax with high precision to preserve accuracy
            val perUnitTax = originalItem.totalTax.divide(
                originalItem.quantity.toBigDecimal(),
                MathContext.DECIMAL128
            )

            refundQuantity.toBigDecimal().multiply(perUnitTax)
        }

        // When roundAtSubtotal=false, round each line item's tax before summing
        // When roundAtSubtotal=true, don't round individual items (will be rounded at total level)
        return if (roundAtSubtotal) {
            itemTax
        } else {
            itemTax.setScale(numberOfDecimals, RoundingMode.HALF_UP)
        }
    }
}
