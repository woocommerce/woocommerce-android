package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
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
    @ColumnInfo(defaultValue = "0") val userId: Long = 0,
    val productId: Long,
    val resourceId: Long,
    val dateCreated: Instant,
    val dateModified: Instant,
    val googleCalendarEventId: String,
    val orderId: Long,
    val orderItemId: Long,
    val parentId: Long,
    val personCounts: List<Long>?,
    val localTimezone: String,
    val customerNote: String?,
    @ColumnInfo(defaultValue = "") val attendanceStatus: AttendanceStatus,
    @ColumnInfo(defaultValue = "") val note: String = "",
    @ColumnInfo val location: String? = null,
    @Embedded("order_") val order: BookingOrderInfo
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

        data object InCart : Status {
            override val key = "in-cart"
        }

        data object Failed : Status {
            override val key = "failed"
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
                    InCart.key -> InCart
                    Failed.key -> Failed
                    else -> Unknown(key)
                }
            }
        }
    }

    sealed interface AttendanceStatus {
        val key: String

        data object Attended : AttendanceStatus {
            override val key = "attended"
        }

        data object Unattended : AttendanceStatus {
            override val key = "unattended"
        }

        data class Unknown(override val key: String) : AttendanceStatus

        companion object {
            fun fromKey(key: String): AttendanceStatus {
                return when (key) {
                    Attended.key -> Attended
                    Unattended.key -> Unattended
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

    @TypeConverter
    fun attendanceStatusToString(status: BookingEntity.AttendanceStatus): String = status.key

    @TypeConverter
    fun stringToAttendanceStatus(key: String): BookingEntity.AttendanceStatus {
        return BookingEntity.AttendanceStatus.fromKey(key)
    }
}

val BookingEntity.isCancellable: Boolean
    get() = status !in listOf(
        BookingEntity.Status.Cancelled,
        BookingEntity.Status.InCart,
        BookingEntity.Status.Complete
    )

val BookingEntity.isReschedulable: Boolean
    get() = status !in listOf(
        BookingEntity.Status.Cancelled,
        BookingEntity.Status.Complete,
        BookingEntity.Status.InCart,
        BookingEntity.Status.Failed,
    )

val BookingEntity.isAttendanceStatusEditable: Boolean
    get() = status != BookingEntity.Status.Cancelled
