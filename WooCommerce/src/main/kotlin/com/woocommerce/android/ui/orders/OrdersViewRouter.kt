package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel

interface OrdersViewRouter {
    fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean = false)
    fun openOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNotificationId: Long? = null)
    fun openOrderFulfillment(order: WCOrderModel, isUsingCachedShipmentTrackings: Boolean = false)
    fun openOrderProductList(order: WCOrderModel)
}
