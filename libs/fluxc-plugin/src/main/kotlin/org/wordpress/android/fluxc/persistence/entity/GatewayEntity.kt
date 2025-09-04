package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "GatewayEntity",
    primaryKeys = ["siteId", "gatewayId"]
)
data class GatewayEntity(
    val siteId: LocalId,
    val gatewayId: String,
    val data: String,
)
