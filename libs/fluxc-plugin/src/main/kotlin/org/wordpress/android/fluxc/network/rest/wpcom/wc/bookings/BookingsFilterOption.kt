package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant

sealed interface BookingsFilterOption {
    object TeamMember : BookingsFilterOption

    data class AttendanceStatuses(val values: Set<BookingEntity.AttendanceStatus>) : BookingsFilterOption {
        companion object {
            val DEFAULT = AttendanceStatuses(emptySet())
            val BookingEntity.AttendanceStatus.Companion.any: BookingEntity.AttendanceStatus?
                get() = null
        }
    }

    object PaymentStatus : BookingsFilterOption

    /**
     * Booking type filter.
     *
     * [value] == null means “Any” (no filter is applied); otherwise a concrete [Type] is selected.
     */
    data class BookingType(val value: Type?) : BookingsFilterOption {
        enum class Type { SERVICE, EVENT }
    }

    data class Customer(val customerId: Long, val customerName: String) : BookingsFilterOption

    object Location : BookingsFilterOption

    data class DateRange(
        val before: Instant?,
        val after: Instant?
    ) : BookingsFilterOption

    object ServiceEvent : BookingsFilterOption
}

data class BookingFilters(
    val dateRange: BookingsFilterOption.DateRange? = null,
    val customer: BookingsFilterOption.Customer? = null,
    val teamMember: BookingsFilterOption.TeamMember? = null,
    val attendanceStatuses: BookingsFilterOption.AttendanceStatuses? = BookingsFilterOption.AttendanceStatuses.DEFAULT,
    val paymentStatus: BookingsFilterOption.PaymentStatus? = null,
    val bookingType: BookingsFilterOption.BookingType? = null,
    val location: BookingsFilterOption.Location? = null,
    val serviceEvent: BookingsFilterOption.ServiceEvent? = null,
) {
    val enabledFiltersCount: Int
        get() {
            var count = 0
            if (dateRange != null) count++
            if (customer != null) count++
            if (teamMember != null) count++
            if (attendanceStatuses != null && attendanceStatuses != BookingsFilterOption.AttendanceStatuses.DEFAULT) {
                count++
            }
            if (paymentStatus != null) count++
            if (bookingType?.value != null) count++
            if (location != null) count++
            if (serviceEvent != null) count++
            return count
        }

    companion object {
        val EMPTY = BookingFilters()
    }
}
