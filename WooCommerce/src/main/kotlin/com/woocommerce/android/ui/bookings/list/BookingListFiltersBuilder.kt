package com.woocommerce.android.ui.bookings.list

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

class BookingListFiltersBuilder @Inject constructor(
    private val clock: Clock
) {
    /**
     * Returns a [BookingsFilterOption.DateRange] based on the selected [BookingListTab].
     *
     * We use UTC for the API calls, as the API stores the dates without timezone information, which means that
     * when comparing dates, they are treated as UTC times.
     * See p1759398245019489-slack-C09FHQNQERG
     */
    fun BookingListTab.asDateRangeFilter(): BookingsFilterOption.DateRange? {
        fun todayAtMidnight() = LocalDate.now(clock).atTime(LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC).toInstant()
        fun todayAtEndOfDay() = LocalDate.now(clock).atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant()

        return when (this) {
            BookingListTab.Today -> BookingsFilterOption.DateRange(
                before = todayAtEndOfDay(),
                after = todayAtMidnight()
            )

            BookingListTab.Upcoming -> BookingsFilterOption.DateRange(
                before = null,
                after = todayAtEndOfDay()
            )

            BookingListTab.All -> null
        }
    }

    fun BookingFilters.asList(): List<BookingsFilterOption> {
        val filters = mutableListOf<BookingsFilterOption>()
        customer?.let { filters.add(it) }
        dateRange?.let { filters.add(it) }
        teamMember?.let { filters.add(it) }
        attendanceStatus?.let { filters.add(it) }
        paymentStatus?.let { filters.add(it) }
        bookingType?.let { filters.add(it) }
        category?.let { filters.add(it) }
        serviceEvent?.let { filters.add(it) }
        return filters
    }
}
