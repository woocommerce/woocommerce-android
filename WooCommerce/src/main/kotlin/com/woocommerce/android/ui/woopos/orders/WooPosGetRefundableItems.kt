package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Get a list of refundable items for an order, taking into account previous refunds.
 *
 * @returns A list of [WooPosRefundableItem] representing each refundable unit. In case line item has quantity > 1,
 * each unit will be represented as a separate [WooPosRefundableItem] with a unique rowIndex.
 */
class WooPosGetRefundableItems @Inject constructor(
    private val currencyFormatter: CurrencyFormatter
) {
    operator fun invoke(
        order: Order,
        refunds: List<Refund>
    ): List<WooPosRefundableItem> {
        val productItems = order.items.filter { it.productId != 0L }

        if (productItems.isEmpty()) {
            return emptyList()
        }

        val maxQuantities: Map<Long, Float> = calculateMaxRefundQuantities(refunds, productItems)

        // Expand grouped items (items with quantity > 1) into individual rows
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
                    refundedItem.orderItemId == orderItem.itemId
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
        return item.price
    }

    private fun calculateUnitTax(item: Order.Item): BigDecimal {
        return if (item.quantity == 0f) {
            item.totalTax
        } else {
            item.totalTax.divide(item.quantity.toBigDecimal(), 2, RoundingMode.HALF_UP)
        }
    }
}
