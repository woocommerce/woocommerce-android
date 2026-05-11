package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject
import kotlin.math.abs

/**
 * Get a list of refundable items for an order, taking into account previous refunds.
 *
 * @param order The order to get refundable items from
 * @param refunds List of previous refunds for this order
 * @param numberOfDecimals Number of decimal places to use for rounding (from store settings)
 * @returns A list of [WooPosRefundableItem] representing each refundable unit. In case line item has quantity > 1,
 * each unit will be represented as a separate [WooPosRefundableItem] with a unique rowIndex.
 */
class WooPosGetRefundableItems @Inject constructor(
    private val currencyFormatter: CurrencyFormatter
) {
    operator fun invoke(
        order: Order,
        refunds: List<Refund>,
    ): List<WooPosRefundableItem> {
        val productRows = buildProductRows(order, refunds)
        val feeRows = buildFeeRows(order, refunds)
        return productRows + feeRows
    }

    private fun buildProductRows(order: Order, refunds: List<Refund>): List<WooPosRefundableItem> {
        if (order.items.isEmpty()) return emptyList()

        val maxQuantities: Map<Long, Float> = calculateMaxRefundQuantities(refunds, order.items)

        // Expand grouped items (items with quantity > 1) into individual rows
        return order.items.flatMap { orderItem ->
            val maxQuantity = maxQuantities[orderItem.itemId]?.toInt() ?: 0

            if (maxQuantity <= 0) {
                emptyList()
            } else {
                val unitPrice = orderItem.price
                val unitTax = calculateUnitTax(orderItem)
                val formattedUnitPrice = PriceUtils.formatCurrency(unitPrice, order.currency, currencyFormatter)
                val formattedUnitTax = PriceUtils.formatCurrency(unitTax, order.currency, currencyFormatter)

                (0 until maxQuantity).map { index ->
                    WooPosRefundableItem(
                        orderItemId = orderItem.itemId,
                        productId = orderItem.productId,
                        variationId = orderItem.variationId,
                        name = orderItem.name,
                        unitPrice = orderItem.price,
                        unitTax = unitTax,
                        formattedUnitPrice = formattedUnitPrice,
                        formattedUnitTax = formattedUnitTax,
                        rowIndex = index,
                    )
                }
            }
        }
    }

    private fun buildFeeRows(order: Order, refunds: List<Refund>): List<WooPosRefundableItem> {
        if (order.feesLines.isEmpty()) return emptyList()
        val refundedFeeIds = refunds.flatMap { it.feeLines }.map { it.id }.toSet()
        return order.feesLines
            .filter { it.id !in refundedFeeIds }
            .map { feeLine ->
                WooPosRefundableItem(
                    orderItemId = feeLine.id,
                    productId = 0L,
                    variationId = 0L,
                    name = feeLine.name.orEmpty(),
                    unitPrice = feeLine.total,
                    unitTax = feeLine.totalTax,
                    formattedUnitPrice = PriceUtils.formatCurrency(feeLine.total, order.currency, currencyFormatter),
                    formattedUnitTax = PriceUtils.formatCurrency(feeLine.totalTax, order.currency, currencyFormatter),
                    rowIndex = 0,
                    isLumpSum = true,
                )
            }
    }

    /**
     * Calculate remaining refundable quantities for each order item.
     */
    private fun calculateMaxRefundQuantities(
        refunds: List<Refund>,
        productItems: List<Order.Item>
    ): Map<Long, Float> {
        val refundedByItemId = refunds
            .flatMap { it.items }
            .groupingBy { it.orderItemId }
            .fold(0f) { acc, item -> acc + item.quantity }

        return productItems
            .associate { it.itemId to (it.quantity - abs(refundedByItemId[it.itemId] ?: 0f)) }
            .filterValues { it > 0 }
    }

    private fun calculateUnitTax(item: Order.Item): BigDecimal {
        return if (item.quantity == 0f) {
            item.totalTax
        } else {
            item.totalTax.divide(item.quantity.toBigDecimal(), MathContext.DECIMAL128)
        }
    }
}
