package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@Entity(
    tableName = "BookingResources",
    primaryKeys = ["id", "localSiteId"]
)
data class BookingResourceEntity(
    val id: LocalOrRemoteId.RemoteId,
    val localSiteId: LocalOrRemoteId.LocalId,
    val name: String,
    val qty: Int,
    val role: String?,
    val email: String?,
    val phoneNumber: String?,
    val imageId: Long,
    val imageUrl: String?,
    val description: String?,
    val productBookingIds: List<Long>? = null,
) {
    companion object // Used for creating extension properties for BookingResourceEntity
}
