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
    val status: Status,
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
) {
    sealed interface Status {
        val key: String

        data object Unpaid : Status {
            override val key = "unpaid"
        }

        data object PendingConfirmation : Status {
            override val key = "pending-confirmation"
        }

        data object Confirmed : Status {
            override val key = "confirmed"
        }

        data object Paid : Status {
            override val key = "paid"
        }

        data object Cancelled : Status {
            override val key = "cancelled"
        }

        data object Complete : Status {
            override val key = "complete"
        }

        data class Unknown(override val key: String) : Status

        companion object Companion {
            fun fromKey(key: String): Status {
                return when (key) {
                    Unpaid.key -> Unpaid
                    PendingConfirmation.key -> PendingConfirmation
                    Confirmed.key -> Confirmed
                    Paid.key -> Paid
                    Cancelled.key -> Cancelled
                    Complete.key -> Complete
                    else -> Unknown(key)
                }
            }
        }
    }
}

internal class BookingEntityConverters {
    @TypeConverter
    fun instantToEpochSeconds(instant: Instant) = instant.epochSecond

    @TypeConverter
    fun epochSecondsToInstant(epochSeconds: Long) = Instant.ofEpochSecond(epochSeconds)

    @TypeConverter
    fun paymentStatusToString(status: BookingEntity.Status): String = status.key

    @TypeConverter
    fun stringToPaymentStatus(key: String): BookingEntity.Status = BookingEntity.Status.fromKey(key)
}
