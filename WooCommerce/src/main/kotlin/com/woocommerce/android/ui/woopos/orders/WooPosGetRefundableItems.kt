package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import javax.inject.Inject

class WooPosGetRefundableItems @Inject constructor(
    private val currencyFormatter: CurrencyFormatter
) {
    operator fun invoke(
        order: Order,
        refunds: List<Refund>
    ): List<WooPosRefundableItem> {
        // Step 1: Filter to only product items (exclude fees, shipping, etc.)
        val productItems = order.items.filter { it.productId != 0L }

        if (productItems.isEmpty()) {
            return emptyList()
        }

        // Step 2: Calculate max refundable quantities
        val maxQuantities: Map<Long, Float> = calculateMaxRefundQuantities(refunds, productItems)

        // Step 3: Expand items into individual rows
        return productItems.flatMap { orderItem ->
            val maxQuantity = maxQuantities[orderItem.itemId]?.toInt() ?: 0

            if (maxQuantity <= 0) {
                emptyList()
            } else {
                val unitPrice = calculateUnitPrice(orderItem)
                val unitTax = calculateUnitTax(orderItem)
                val formattedUnitPrice = PriceUtils.formatCurrency(unitPrice, order.currency, currencyFormatter)
                val formattedUnitTax = PriceUtils.formatCurrency(unitTax, order.currency, currencyFormatter)

                (0 until maxQuantity).map { index ->
                    WooPosRefundableItem(
                        orderItemId = orderItem.itemId,
                        productId = orderItem.productId,
                        variationId = orderItem.variationId,
                        name = orderItem.name,
                        unitPrice = unitPrice,
                        unitTax = unitTax,
                        formattedUnitPrice = formattedUnitPrice,
                        formattedUnitTax = formattedUnitTax,
                        rowIndex = index
                    )
                }
            }
        }
    }

    /**
     * Calculate remaining refundable quantities for each order item.
     */
    private fun calculateMaxRefundQuantities(
        refunds: List<Refund>,
        productItems: List<Order.Item>
    ): Map<Long, Float> {
        val allRefundedItems = refunds.flatMap { it.items }

        return productItems.mapNotNull { orderItem ->
            val refundedQuantity = allRefundedItems
                .filter { refundedItem ->
                    refundedItem.productId == orderItem.productId &&
                        refundedItem.variationId == orderItem.variationId
                }
                .sumOf { it.quantity }

            val remainingQuantity = orderItem.quantity - refundedQuantity

            if (remainingQuantity > 0) {
                orderItem.itemId to remainingQuantity
            } else {
                null
            }
        }.toMap()
    }

    private fun calculateUnitPrice(item: Order.Item): BigDecimal {
        return if (item.quantity == 0f) {
            item.total
        } else {
            item.total / item.quantity.toBigDecimal()
        }
    }

    private fun calculateUnitTax(item: Order.Item): BigDecimal {
        return if (item.quantity == 0f) {
            item.totalTax
        } else {
            item.totalTax / item.quantity.toBigDecimal()
        }
    }
}
