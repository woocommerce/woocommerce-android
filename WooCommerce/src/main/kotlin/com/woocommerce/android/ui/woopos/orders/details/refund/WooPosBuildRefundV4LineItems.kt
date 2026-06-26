package com.woocommerce.android.ui.woopos.orders.details.refund

import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import javax.inject.Inject

class WooPosBuildRefundV4LineItems @Inject constructor() {
    operator fun invoke(selectedItems: List<WooPosRefundableItem>): List<RefundV4LineItem> {
        val (feeItems, productItems) = selectedItems.partition { it.isLumpSum }

        val productLineItems = productItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, rows) ->
                RefundV4LineItem.quantityBased(lineItemId = orderItemId, quantity = rows.size)
            }

        val feeLineItems = feeItems.map { fee ->
            RefundV4LineItem.amountBased(
                lineItemId = fee.orderItemId,
                refundTotal = fee.unitPrice + fee.unitTax,
            )
        }

        return productLineItems + feeLineItems
    }
}
