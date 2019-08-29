package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

/**
 * Interface for handling order refund actions from a child fragment.
 */
interface OrderRefundActionListener {
    fun issueOrderRefund(order: WCOrderModel)
}
