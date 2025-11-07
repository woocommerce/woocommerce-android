package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.AttendanceStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.BookingType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.Customer
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.DateRange
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.Location
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.PaymentStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.ServiceEvent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.TeamMember
import java.time.Instant

sealed interface BookingsFilterOption {
    object TeamMember : BookingsFilterOption

    object AttendanceStatus : BookingsFilterOption

    object PaymentStatus : BookingsFilterOption

    data class BookingType(val value: Type) : BookingsFilterOption {
        enum class Type { ANY, SERVICE, EVENT }

        companion object {
            val DEFAULT = BookingType(Type.ANY)
        }
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
    val dateRange: DateRange? = null,
    val customer: Customer? = null,
    val teamMember: TeamMember? = null,
    val attendanceStatus: AttendanceStatus? = null,
    val paymentStatus: PaymentStatus? = null,
    val bookingType: BookingType = BookingType.DEFAULT,
    val location: Location? = null,
    val serviceEvent: ServiceEvent? = null,
) {
    val enabledFiltersCount: Int
        get() {
            var count = 0
            if (dateRange != null) count++
            if (customer != null) count++
            if (teamMember != null) count++
            if (attendanceStatus != null) count++
            if (paymentStatus != null) count++
            if (bookingType.value != BookingType.DEFAULT.value) count++
            if (location != null) count++
            if (serviceEvent != null) count++
            return count
        }

    companion object {
        val EMPTY = BookingFilters()
    }
}
