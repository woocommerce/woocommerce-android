package com.woocommerce.android.ui.woopos.orders.details.refund

import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import javax.inject.Inject

/**
 * Maps the user's selected refundable rows into the simplified v4 refund line items.
 *
 * The client describes only *what* to refund — the server computes the monetary values:
 * - Product rows are grouped by order item id and sent as `{line_item_id, quantity}`.
 * - Fee rows (lump sum, no quantity concept) are sent as `{line_item_id, refund_total}` with the
 *   tax-inclusive amount (`unitPrice + unitTax`); the server splits the tax out via the line's
 *   stored ratio.
 */
class WooPosBuildRefundV4LineItems @Inject constructor() {
    operator fun invoke(selectedItems: List<WooPosRefundableItem>): List<RefundV4LineItem> {
        val (feeItems, productItems) = selectedItems.partition { it.isLumpSum }

        val productLineItems = productItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, rows) ->
                RefundV4LineItem(lineItemId = orderItemId, quantity = rows.size)
            }

        val feeLineItems = feeItems.map { fee ->
            RefundV4LineItem(
                lineItemId = fee.orderItemId,
                refundTotal = fee.unitPrice + fee.unitTax,
            )
        }

        return productLineItems + feeLineItems
    }
}
