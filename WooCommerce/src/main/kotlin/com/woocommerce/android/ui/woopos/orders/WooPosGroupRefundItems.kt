package com.woocommerce.android.ui.woopos.orders

import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import javax.inject.Inject

class WooPosGroupRefundItems @Inject constructor() {
    operator fun invoke(refundableItems: List<WooPosRefundableItem>): List<RefundRequestItem> {
        return refundableItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                RefundRequestItem(
                    itemId = orderItemId,
                    quantity = items.size
                )
            }
    }
}
