package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

@Parcelize
data class OrderShipmentTracking(
    val localSiteId: Int = 0,
    val orderId: Long = 0,
    val remoteTrackingId: String = "",
    val trackingNumber: String,
    val trackingProvider: String,
    val trackingLink: String = "",
    val dateShipped: String,
    val isCustomProvider: Boolean = false
) : Parcelable {
    fun toDataModel() = WCOrderShipmentTrackingModel(
        localSiteId = LocalId(this.localSiteId),
        orderId = RemoteId(this.orderId),
        remoteTrackingId = this.remoteTrackingId,
        trackingNumber = this.trackingNumber,
        trackingProvider = this.trackingProvider,
        trackingLink = this.trackingLink,
        dateShipped = this.dateShipped
    )
}

fun WCOrderShipmentTrackingModel.toAppModel(): OrderShipmentTracking {
    return OrderShipmentTracking(
        localSiteId = localSiteId.value,
        orderId = orderId.value,
        remoteTrackingId = remoteTrackingId,
        trackingNumber = trackingNumber,
        trackingProvider = trackingProvider,
        trackingLink = trackingLink,
        dateShipped = dateShipped
    )
}
