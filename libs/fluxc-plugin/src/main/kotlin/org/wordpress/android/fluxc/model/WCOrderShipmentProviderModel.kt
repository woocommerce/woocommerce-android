package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "OrderShipmentProviderEntity",
    primaryKeys = ["localSiteId", "carrierName", "country"]
)
data class WCOrderShipmentProviderModel(
    val localSiteId: LocalId,
    val country: String = "",
    val carrierName: String = "",
    val carrierLink: String = ""
)
