package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

/**
 * Interface for handling individual order actions from a child fragment.
 */
interface OrderActionListener {
    fun openOrderFulfillment(order: WCOrderModel)
    fun openOrderProductList(order: WCOrderModel)
    fun openRefundedProductList(order: WCOrderModel)
}
