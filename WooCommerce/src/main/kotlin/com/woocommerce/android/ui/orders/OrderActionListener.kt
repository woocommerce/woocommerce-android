package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

/**
 * Interface for handling individual order actions from a child fragment.
 */
interface OrderActionListener {
    fun viewOrderFulfillment(order: WCOrderModel)
    fun viewProductList(order: WCOrderModel)
}
