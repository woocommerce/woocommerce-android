package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "TopPerformerCategories",
    primaryKeys = ["datePeriod", "categoryId", "localSiteId"]
)
data class TopPerformerCategoryEntity(
    val localSiteId: LocalId,
    val datePeriod: String,
    val categoryId: RemoteId,
    val name: String,
    val quantity: Int,
    val currency: String,
    val total: Double,
    val millisSinceLastUpdated: Long
)
