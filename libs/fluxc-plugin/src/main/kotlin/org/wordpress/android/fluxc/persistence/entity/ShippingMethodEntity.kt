package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCShippingMethod

@Entity(tableName = "ShippingMethod", primaryKeys = ["localSiteId", "id"])
data class ShippingMethodEntity (
    val id: String,
    val localSiteId: LocalId,
    val title: String
)

fun WCShippingMethod.toEntity(localSiteId: LocalId) = ShippingMethodEntity(
    id = this.id,
    localSiteId = localSiteId,
    title = this.title
)
