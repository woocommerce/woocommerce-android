package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "OrderShipmentTrackingEntity",
    primaryKeys = ["localSiteId", "orderId", "remoteTrackingId"]
)
data class WCOrderShipmentTrackingModel(
    val localSiteId: LocalId,
    val orderId: RemoteId,
    val remoteTrackingId: String,
    val trackingNumber: String,
    val trackingProvider: String,
    val trackingLink: String,
    val dateShipped: String
)
