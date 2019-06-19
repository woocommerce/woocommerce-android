package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

interface OrderShipmentTrackingActionListener {
    fun openAddOrderShipmentTrackingScreen()
    fun deleteOrderShipmentTracking(item: WCOrderShipmentTrackingModel)
}
