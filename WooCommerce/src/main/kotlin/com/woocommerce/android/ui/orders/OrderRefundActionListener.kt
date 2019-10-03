package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Order

/**
 * Interface for handling order refund actions from a child fragment.
 */
interface OrderRefundActionListener {
    fun issueOrderRefund(order: Order)
    fun showRefundDetail(orderId: Long, refundId: Long)
}
