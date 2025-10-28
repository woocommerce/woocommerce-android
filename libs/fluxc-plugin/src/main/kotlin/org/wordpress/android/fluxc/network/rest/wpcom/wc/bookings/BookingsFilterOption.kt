package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import java.time.Instant

sealed interface BookingsFilterOption {
    object TeamMember : BookingsFilterOption

    object AttendanceStatus : BookingsFilterOption

    object PaymentStatus : BookingsFilterOption

    object BookingType : BookingsFilterOption

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
    val attendanceStatus: BookingsFilterOption.AttendanceStatus? = null,
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
            if (attendanceStatus != null) count++
            if (paymentStatus != null) count++
            if (bookingType != null) count++
            if (location != null) count++
            if (serviceEvent != null) count++
            return count
        }
}
