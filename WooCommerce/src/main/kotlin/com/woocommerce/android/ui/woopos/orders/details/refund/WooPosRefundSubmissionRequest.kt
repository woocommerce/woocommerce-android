package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import java.math.BigDecimal

data class WooPosRefundSubmissionRequest(
    val order: Order,
    val refundAmount: BigDecimal,
    val refundReason: String,
    val refundItems: List<RefundRequestItem>,
    // Simplified v4 line items. Non-null when v4 is available for the site: the backend computes all
    // monetary values, so no client-calculated amount is sent. Null means use the v3 path with
    // [refundItems] + [refundAmount].
    val v4LineItems: List<RefundV4LineItem>? = null,
    val cardRefundAlreadySucceeded: Boolean = false,
) {
    val orderId: Long = order.id
}
