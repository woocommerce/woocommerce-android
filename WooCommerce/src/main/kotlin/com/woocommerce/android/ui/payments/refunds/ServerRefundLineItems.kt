package com.woocommerce.android.ui.payments.refunds

import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem

/**
 * Lines for the refund preview. Products are sent with the chosen quantity. Shipping and fee lines are only refunded
 * whole, so they are sent with a quantity of 1 and the server refunds what is left on them.
 */
fun List<RefundItem>.toRefundPreviewLineItems(): List<RefundPreviewLineItem> =
    serverRefundQuantities().map { (itemId, quantity) ->
        RefundPreviewLineItem.quantityBased(lineItemId = itemId, quantity = quantity)
    }

/** Same lines as [toRefundPreviewLineItems], for the refund created with `compute_totals`. */
fun List<RefundItem>.toComputedRefundLineItems(): List<ComputedRefundLineItem> =
    serverRefundQuantities().map { (itemId, quantity) ->
        ComputedRefundLineItem.quantityBased(lineItemId = itemId, quantity = quantity)
    }

/**
 * The server rejects a line that refunds nothing, so products with no quantity chosen and zero-total shipping or fee
 * lines, like free shipping, are left out. Lines with a negative total, like a discount fee, are kept.
 */
private fun List<RefundItem>.serverRefundQuantities(): List<Pair<Long, Int>> = mapNotNull { item ->
    when (item) {
        is ProductRefundListItem -> item.quantity.takeIf { it > 0 }
        is ShippingRefundListItem, is FeeRefundListItem -> 1.takeIf { item.total.signum() != 0 }
    }?.let { quantity -> item.itemId to quantity }
}
