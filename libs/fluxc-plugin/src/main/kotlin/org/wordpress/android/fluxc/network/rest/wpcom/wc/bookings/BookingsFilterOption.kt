package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import java.time.Instant

sealed interface BookingsFilterOption {
    object TeamMember : BookingsFilterOption

    object AttendanceStatus : BookingsFilterOption

    object PaymentStatus : BookingsFilterOption

    object BookingType : BookingsFilterOption

    data class Customer(val customerId: Long?) : BookingsFilterOption

    object Category : BookingsFilterOption

    data class DateRange(
        val before: Instant?,
        val after: Instant?
    ) : BookingsFilterOption

    object ServiceEvent : BookingsFilterOption
}
