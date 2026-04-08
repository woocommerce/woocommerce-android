package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant

sealed interface BookingsFilterOption {
    data class TeamMembers(val values: Set<LocalOrRemoteId.RemoteId>) : BookingsFilterOption {
        companion object {
            val DEFAULT = TeamMembers(emptySet())
        }
    }

    /**
     * Booking type filter.
     *
     * [value] == null means “Any” (no filter is applied); otherwise a concrete [Type] is selected.
     */
    data class BookingType(val value: Type?) : BookingsFilterOption {
        enum class Type { SERVICE, EVENT }
    }

    data class AttendanceStatus(val value: BookingEntity.AttendanceStatus?) : BookingsFilterOption {
        companion object {
            val DEFAULT = AttendanceStatus(null)
        }
    }

    data class ExcludedBookingStatuses(val values: Set<BookingEntity.Status>) : BookingsFilterOption {
        companion object {
            val DEFAULT = ExcludedBookingStatuses(emptySet())
        }
    }

    object PaymentStatus : BookingsFilterOption

    data class Customer(val userId: Long, val customerName: String) : BookingsFilterOption

    data class DateRange(
        val before: Instant?,
        val after: Instant?,
    ) : BookingsFilterOption {
        companion object {
            val DEFAULT = DateRange(null, null)
        }
    }

    object Location : BookingsFilterOption

    data class ServiceEvents(val values: Set<ProductInfo>) : BookingsFilterOption {
        companion object {
            val DEFAULT = ServiceEvents(emptySet())
        }
    }

    data class ProductInfo(
        val productId: Long,
        val productName: String
    )
}

data class BookingFilters(
    val teamMembers: BookingsFilterOption.TeamMembers = BookingsFilterOption.TeamMembers.DEFAULT,
    val bookingType: BookingsFilterOption.BookingType? = null,
    val serviceEvents: BookingsFilterOption.ServiceEvents = BookingsFilterOption.ServiceEvents.DEFAULT,
    val attendanceStatus: BookingsFilterOption.AttendanceStatus = BookingsFilterOption.AttendanceStatus.DEFAULT,
    val excludedBookingStatuses: BookingsFilterOption.ExcludedBookingStatuses = BookingsFilterOption.ExcludedBookingStatuses.DEFAULT,
    val paymentStatus: BookingsFilterOption.PaymentStatus? = null,
    val customer: BookingsFilterOption.Customer? = null,
    val dateRange: BookingsFilterOption.DateRange = BookingsFilterOption.DateRange.DEFAULT,
    val location: BookingsFilterOption.Location? = null,
) {
    val enabledFiltersCount: Int
        get() {
            var count = 0
            if (teamMembers != BookingsFilterOption.TeamMembers.DEFAULT) count++
            if (bookingType?.value != null) count++
            if (serviceEvents != BookingsFilterOption.ServiceEvents.DEFAULT) count++
            if (attendanceStatus != BookingsFilterOption.AttendanceStatus.DEFAULT) count++
            if (paymentStatus != null) count++
            if (customer != null) count++
            if (dateRange != BookingsFilterOption.DateRange.DEFAULT) count++
            if (location != null) count++
            return count
        }

    companion object {
        val EMPTY = BookingFilters()
    }
}
