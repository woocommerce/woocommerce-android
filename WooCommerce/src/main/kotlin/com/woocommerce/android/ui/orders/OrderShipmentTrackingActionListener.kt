package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

interface OrderShipmentTrackingActionListener {
    /**
     * This method is needed only in [OrderFulfillmentFragment] and not in [OrderDetailFragment]
     * so adding a default implementation here to mark the method as optional so that
     * implementing classes do not have to implement this method if not needed
     */
    fun openAddOrderShipmentTrackingScreen() { }
    fun deleteOrderShipmentTracking(item: WCOrderShipmentTrackingModel)
}
