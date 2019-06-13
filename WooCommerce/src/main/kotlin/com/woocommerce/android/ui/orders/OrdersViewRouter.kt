package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrdersViewRouter {
    fun openAddOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        orderTrackingProvider: String,
        isCustomProvider: Boolean
    )
}
