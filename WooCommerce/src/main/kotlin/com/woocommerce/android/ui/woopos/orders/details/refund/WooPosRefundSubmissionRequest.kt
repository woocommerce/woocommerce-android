package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import java.math.BigDecimal

/**
 * [serverLineItems] discriminates the backend path: when non-null the refund is created through
 * the server-computed flow (`compute_totals=true`, only set after a successful preview confirmed
 * support); when null the classic v3 item refund built from [refundItems] is sent.
 */
data class WooPosRefundSubmissionRequest(
    val order: Order,
    val refundAmount: BigDecimal,
    val refundReason: String,
    val refundItems: List<RefundRequestItem>,
    val serverLineItems: List<ComputedRefundLineItem>? = null,
    val cardRefundAlreadySucceeded: Boolean = false,
) {
    val orderId: Long = order.id
}
