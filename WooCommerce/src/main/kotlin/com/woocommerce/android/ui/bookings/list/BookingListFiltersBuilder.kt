package com.woocommerce.android.ui.bookings.list

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsDateRangePresets
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import javax.inject.Inject

class BookingListFiltersBuilder @Inject constructor(
    private val clock: Clock
) {
    /**
     * Returns a [BookingsFilterOption.DateRange] based on the selected [BookingListTab].
     */
    fun BookingListTab.asDateRangeFilter(): BookingsFilterOption.DateRange = when (this) {
        BookingListTab.Today -> BookingsDateRangePresets.today(clock)
        BookingListTab.Upcoming -> BookingsDateRangePresets.upcoming(clock)
        BookingListTab.All -> BookingsFilterOption.DateRange.DEFAULT
    }
}
