package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import java.time.Instant

@Entity(
    tableName = "Bookings",
    primaryKeys = ["id", "localSiteId"]
)
@TypeConverters(BookingEntityConverters::class)
data class BookingEntity(
    val id: RemoteId,
    val localSiteId: LocalId,
    val start: Instant,
    val end: Instant,
    val allDay: Boolean,
    val status: String,
    val cost: String,
    val currency: String,
    val customerId: Long,
    val productId: Long,
    val resourceId: Long,
    val dateCreated: Instant,
    val dateModified: Instant,
    val googleCalendarEventId: String,
    val orderId: Long,
    val orderItemId: Long,
    val parentId: Long,
    val personCounts: List<Long>?,
    val localTimezone: String
)

internal class BookingEntityConverters {
    @TypeConverter
    fun instantToEpochSeconds(instant: Instant) = instant.epochSecond

    @TypeConverter
    fun epochSecondsToInstant(epochSeconds: Long) = Instant.ofEpochSecond(epochSeconds)
}
