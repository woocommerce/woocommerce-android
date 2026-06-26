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
    val v4LineItems: List<RefundV4LineItem>? = null,
    val cardRefundAlreadySucceeded: Boolean = false,
) {
    val orderId: Long = order.id
}
