package com.woocommerce.android.ui.bookings.list

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

class BookingListFiltersBuilder @Inject constructor(
    private val clock: Clock
) {
    fun BookingListTab.asDateRangeFilter(): BookingsFilterOption.DateRange? {
        return when (this) {
            BookingListTab.Today -> BookingsFilterOption.DateRange(
                before = ZonedDateTime.now(clock).with(LocalTime.MAX).toInstant(),
                after = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
            )

            BookingListTab.Upcoming -> BookingsFilterOption.DateRange(
                before = null,
                after = Instant.now(clock)
            )

            BookingListTab.All -> null
        }
    }
}
