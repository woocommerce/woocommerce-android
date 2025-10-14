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
)
