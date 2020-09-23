package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@Parcelize
data class OrderShipmentTracking(
    val localSiteId: Int,
    val localOrderId: Int,
    val remoteTrackingId: String,
    val trackingNumber: String,
    val trackingProvider: String,
    val trackingLink: String,
    val dateShipped: Date
) : Parcelable

fun WCOrderShipmentTrackingModel.toAppModel(): OrderShipmentTracking {
    return OrderShipmentTracking(
        localSiteId,
        localOrderId,
        remoteTrackingId,
        trackingNumber,
        trackingProvider,
        trackingLink,
        DateTimeUtils.dateUTCFromIso8601(this.dateShipped) ?: Date()
    )
}
