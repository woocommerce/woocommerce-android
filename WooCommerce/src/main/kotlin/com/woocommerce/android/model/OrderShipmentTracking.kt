package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

@Parcelize
data class OrderShipmentTracking(
    val id: Int = 0,
    val localSiteId: Int = 0,
    val localOrderId: Int = 0,
    val remoteTrackingId: String = "",
    val trackingNumber: String,
    val trackingProvider: String,
    val trackingLink: String = "",
    val dateShipped: String,
    val isCustomProvider: Boolean = false
) : Parcelable {
    fun toDataModel() = WCOrderShipmentTrackingModel().also { orderShipmentTrackingModel ->
        orderShipmentTrackingModel.id = this.id
        orderShipmentTrackingModel.localOrderId = this.localOrderId
        orderShipmentTrackingModel.localSiteId = this.localSiteId
        orderShipmentTrackingModel.remoteTrackingId = this.remoteTrackingId
        orderShipmentTrackingModel.trackingNumber = this.trackingNumber
        orderShipmentTrackingModel.dateShipped = this.dateShipped
        orderShipmentTrackingModel.trackingProvider = this.trackingProvider
    }
}

fun WCOrderShipmentTrackingModel.toAppModel(): OrderShipmentTracking {
    return OrderShipmentTracking(
        id,
        localSiteId,
        localOrderId,
        remoteTrackingId,
        trackingNumber,
        trackingProvider,
        trackingLink,
        dateShipped
    )
}
