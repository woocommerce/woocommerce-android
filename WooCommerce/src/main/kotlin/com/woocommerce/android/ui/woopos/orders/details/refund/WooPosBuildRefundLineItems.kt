package com.woocommerce.android.ui.woopos.orders.details.refund

import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Builds the line items describing *what* to refund for the server-computed flow: quantity-based
 * lines for products (grouped by order item) and amount-based lines for fees/shipping.
 *
 * The preview and the computed create carry the same information but serialize the line item id
 * under different keys (`line_item_id` vs `id`), hence the two entry points sharing one builder.
 */
class WooPosBuildRefundLineItems @Inject constructor() {
    fun forPreview(selectedItems: List<WooPosRefundableItem>): List<RefundPreviewLineItem> =
        build(
            selectedItems = selectedItems,
            quantityBased = { lineItemId, quantity ->
                RefundPreviewLineItem.quantityBased(lineItemId = lineItemId, quantity = quantity)
            },
            amountBased = { lineItemId, refundTotal ->
                RefundPreviewLineItem.amountBased(lineItemId = lineItemId, refundTotal = refundTotal)
            },
        )

    fun forComputedCreate(selectedItems: List<WooPosRefundableItem>): List<ComputedRefundLineItem> =
        build(
            selectedItems = selectedItems,
            quantityBased = { lineItemId, quantity ->
                ComputedRefundLineItem.quantityBased(lineItemId = lineItemId, quantity = quantity)
            },
            amountBased = { lineItemId, refundTotal ->
                ComputedRefundLineItem.amountBased(lineItemId = lineItemId, refundTotal = refundTotal)
            },
        )

    private fun <T> build(
        selectedItems: List<WooPosRefundableItem>,
        quantityBased: (lineItemId: Long, quantity: Int) -> T,
        amountBased: (lineItemId: Long, refundTotal: BigDecimal) -> T,
    ): List<T> {
        val (feeItems, productItems) = selectedItems.partition { it.isLumpSum }

        val productLineItems = productItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, rows) -> quantityBased(orderItemId, rows.size) }

        val feeLineItems = feeItems.map { fee -> amountBased(fee.orderItemId, fee.unitPrice + fee.unitTax) }

        return productLineItems + feeLineItems
    }
}
