package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "OrderFulfillmentEntity",
    primaryKeys = ["localSiteId", "orderId", "fulfillmentId"]
)
data class WCOrderFulfillmentModel(
    val localSiteId: LocalId,
    val orderId: RemoteId,
    val fulfillmentId: Long,
    val status: String? = null,
    val isFulfilled: Boolean = false,
    val dateUpdated: String? = null,
    val dateFulfilled: String? = null,
    val trackingNumber: String? = null,
    val shipmentProvider: String? = null,
    val trackingUrl: String? = null
)
