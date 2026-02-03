package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

class WooPosGroupRefundItems @Inject constructor() {
    operator fun invoke(
        refundableItems: List<WooPosRefundableItem>,
        order: Order,
        numberOfDecimals: Int,
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
                    refundTotal = calculateRefundTotal(originalItem, refundQuantity, numberOfDecimals),
                    refundTax = calculateRefundTaxes(originalItem, refundQuantity, numberOfDecimals)
                )
            }
    }

    private fun calculateRefundTotal(
        originalItem: Order.Item,
        quantity: Int,
        numberOfDecimals: Int,
    ): BigDecimal {
        return originalItem.price
            .multiply(quantity.toBigDecimal())
            .setScale(numberOfDecimals, RoundingMode.HALF_UP)
    }

    private fun calculateRefundTaxes(
        originalItem: Order.Item,
        quantity: Int,
        numberOfDecimals: Int,
    ): List<RefundRequestTax> {
        check(originalItem.quantity > 0f) {
            "Order item ${originalItem.itemId} has invalid quantity ${originalItem.quantity}."
        }

        val refundQuantity = quantity.toBigDecimal()
        return originalItem.taxes.map { tax: Order.LineTaxEntry ->
            // Calculate per-unit tax with high precision to preserve accuracy
            val perUnitTax = tax.taxAmount.divide(
                originalItem.quantity.toBigDecimal(),
                MathContext.DECIMAL128
            )

            // Apply rounding to the final refund (tax) total
            val refundTaxTotal = refundQuantity
                .multiply(perUnitTax)
                .setScale(numberOfDecimals, RoundingMode.HALF_UP)

            RefundRequestTax(
                taxRateId = tax.rateId,
                refundTotal = refundTaxTotal
            )
        }
    }
}
