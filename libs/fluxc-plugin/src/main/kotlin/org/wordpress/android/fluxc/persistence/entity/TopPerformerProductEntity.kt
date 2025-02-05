package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "TopPerformerProducts",
    primaryKeys = ["datePeriod", "productId", "localSiteId"]
)
data class TopPerformerProductEntity(
    val localSiteId: LocalId,
    val datePeriod: String,
    val productId: RemoteId,
    val name: String,
    val imageUrl: String?,
    val quantity: Int,
    val currency: String,
    val total: Double,
    val millisSinceLastUpdated: Long
)
