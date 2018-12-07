package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

interface OrdersViewRouter {
    fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean = false)
    fun openOrderDetail(localSiteId: Int, remoteOrderId: Long)
    fun openOrderFulfillment(order: WCOrderModel)
    fun openOrderProductList(order: WCOrderModel)
}
