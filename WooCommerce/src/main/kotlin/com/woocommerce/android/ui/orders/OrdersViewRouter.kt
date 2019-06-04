package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrdersViewRouter {
    fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean = false)
    fun openOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNotificationId: Long? = null)
    fun openOrderFulfillment(order: WCOrderModel, isUsingCachedShipmentTrackings: Boolean = false)
    fun openOrderProductList(order: WCOrderModel)
    fun openAddOrderNote(order: WCOrderModel)
    fun openAddOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        orderTrackingProvider: String,
        isCustomProvider: Boolean
    )
}
