package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosGroupRefundItems @Inject constructor() {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order
    ): List<RefundRequestItem> {
        return refundableItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                val originalItem = requireNotNull(order.items.find { it.itemId == orderItemId }) {
                    "Order item with ID $orderItemId not found in order ${order.id}."
                }
                val refundQuantity = items.size

                RefundRequestItem(
                    itemId = orderItemId,
                    quantity = refundQuantity,
                    refundTotal = calculateRefundTotal(originalItem, refundQuantity),
                    refundTax = calculateRefundTaxes(originalItem, refundQuantity)
                )
            }
    }

    private fun calculateRefundTotal(
        originalItem: Order.Item,
        quantity: Int
    ): BigDecimal {
        return originalItem.price.multiply(quantity.toBigDecimal())
    }

    private fun calculateRefundTaxes(
        originalItem: Order.Item,
        quantity: Int
    ): List<RefundRequestTax> {
        check(originalItem.quantity > 0f) {
            "Order item ${originalItem.itemId} has invalid quantity ${originalItem.quantity}."
        }

        val refundQuantity = quantity.toBigDecimal()

        return originalItem.taxes.map { tax ->
            val singleItemTax = tax.taxAmount.divide(
                originalItem.quantity.toBigDecimal(),
                2,
                RoundingMode.HALF_UP
            )
            RefundRequestTax(
                taxRateId = tax.rateId,
                refundTotal = refundQuantity.multiply(singleItemTax)
            )
        }
    }
}
