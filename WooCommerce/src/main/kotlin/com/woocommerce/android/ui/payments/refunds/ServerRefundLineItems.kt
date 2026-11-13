package com.woocommerce.android.ui.payments.refunds

import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem

fun List<RefundItem>.toRefundPreviewLineItems(): List<RefundPreviewLineItem> =
    serverRefundQuantities().map { (itemId, quantity) ->
        RefundPreviewLineItem.quantityBased(lineItemId = itemId, quantity = quantity)
    }

fun List<RefundItem>.toComputedRefundLineItems(): List<ComputedRefundLineItem> =
    serverRefundQuantities().map { (itemId, quantity) ->
        ComputedRefundLineItem.quantityBased(lineItemId = itemId, quantity = quantity)
    }

// The server rejects a line that refunds nothing.
private fun List<RefundItem>.serverRefundQuantities(): List<Pair<Long, Int>> = mapNotNull { item ->
    when (item) {
        is ProductRefundListItem -> item.quantity.takeIf { it > 0 }
        // Quantity 1 makes the server refund what is left on the shipping or fee line.
        is ShippingRefundListItem, is FeeRefundListItem -> 1.takeIf { item.total.signum() != 0 }
    }?.let { quantity -> item.itemId to quantity }
}
