package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.*
import java.time.Instant

sealed interface BookingsFilterOption {
    object TeamMember : BookingsFilterOption

    object AttendanceStatus : BookingsFilterOption

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
    val dateRange: DateRange? = null,
    val customer: Customer? = null,
    val teamMember: TeamMember? = null,
    val attendanceStatus: AttendanceStatus? = null,
    val paymentStatus: PaymentStatus? = null,
    val bookingType: BookingType? = null,
    val location: Location? = null,
    val serviceEvents: ServiceEvents = ServiceEvents.DEFAULT,
    ) {
    val enabledFiltersCount: Int
        get() {
            var count = 0
            if (dateRange != null) count++
            if (customer != null) count++
            if (teamMember != null) count++
            if (attendanceStatus != null) count++
            if (paymentStatus != null) count++
            if (bookingType?.value != null) count++
            if (location != null) count++
            if (serviceEvents != ServiceEvents.DEFAULT) count++
            return count
        }

    companion object {
        val EMPTY = BookingFilters()
    }
}
