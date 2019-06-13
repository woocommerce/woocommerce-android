package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrdersViewRouter {
    fun openOrderProductList(order: WCOrderModel)
    fun openAddOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        orderTrackingProvider: String,
        isCustomProvider: Boolean
    )
}
