package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "Bookings",
    primaryKeys = ["id", "localSiteId"]
)
data class BookingEntity(
    val id: RemoteId,
    val localSiteId: LocalId,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val status: String,
    val cost: String,
    val currency: String,
    val customerId: Long,
    val productId: Long,
    val resourceId: Long,
    val dateCreated: Long,
    val dateModified: Long,
    val googleCalendarEventId: String,
    val orderId: Long,
    val orderItemId: Long,
    val parentId: Long,
    val personCounts: List<Long>?,
    val localTimezone: String
)
