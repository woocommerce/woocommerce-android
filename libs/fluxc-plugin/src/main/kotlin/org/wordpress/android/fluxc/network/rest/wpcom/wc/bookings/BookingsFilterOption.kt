package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import java.time.Instant

sealed interface BookingsFilterOption {
    data class TeamMembers(val values: Set<BookingResourceEntity>) : BookingsFilterOption {
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

    object ServiceEvent : BookingsFilterOption

    data class AttendanceStatuses(val values: Set<BookingEntity.AttendanceStatus>) : BookingsFilterOption {
        companion object {
            val DEFAULT = AttendanceStatuses(emptySet())
        }
    }

    object PaymentStatus : BookingsFilterOption

    data class Customer(val customerId: Long, val customerName: String) : BookingsFilterOption

    data class DateRange(
        val before: Instant?,
        val after: Instant?
    ) : BookingsFilterOption

    object Location : BookingsFilterOption
}

data class BookingFilters(
    val teamMembers: BookingsFilterOption.TeamMembers = BookingsFilterOption.TeamMembers.DEFAULT,
    val bookingType: BookingsFilterOption.BookingType? = null,
    val serviceEvent: BookingsFilterOption.ServiceEvent? = null,
    val attendanceStatuses: BookingsFilterOption.AttendanceStatuses = BookingsFilterOption.AttendanceStatuses.DEFAULT,
    val paymentStatus: BookingsFilterOption.PaymentStatus? = null,
    val customer: BookingsFilterOption.Customer? = null,
    val dateRange: BookingsFilterOption.DateRange? = null,
    val location: BookingsFilterOption.Location? = null,
) {
    val enabledFiltersCount: Int
        get() {
            var count = 0
            if (teamMembers.values != BookingsFilterOption.TeamMembers.DEFAULT) count++
            if (bookingType?.value != null) count++
            if (serviceEvent != null) count++
            if (attendanceStatuses != BookingsFilterOption.AttendanceStatuses.DEFAULT) count++
            if (paymentStatus != null) count++
            if (customer != null) count++
            if (dateRange != null) count++
            if (location != null) count++
            return count
        }

    companion object {
        val EMPTY = BookingFilters()
    }
}
