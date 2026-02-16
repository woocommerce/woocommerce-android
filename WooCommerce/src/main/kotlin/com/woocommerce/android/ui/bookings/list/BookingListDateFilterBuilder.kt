package com.woocommerce.android.ui.bookings.list

import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsDateRangePresets
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import javax.inject.Inject

class BookingListDateFilterBuilder @Inject constructor(
    private val clock: Clock
) {
    fun prepareDateFilter(
        selectedTab: BookingListTab,
        selectedDateRange: BookingsFilterOption.DateRange
    ): BookingsFilterOption.DateRange {
        val tabDateRange = selectedTab.asDateRangeFilter()
        // Merge the tab date range (if any) with the selected date range (if any)
        return BookingsFilterOption.DateRange(
            after = listOfNotNull(tabDateRange.after, selectedDateRange.after).maxOrNull(),
            before = listOfNotNull(tabDateRange.before, selectedDateRange.before).minOrNull()
        )
    }

    private fun BookingListTab.asDateRangeFilter(): BookingsFilterOption.DateRange = when (this) {
        BookingListTab.Today -> BookingsDateRangePresets.today(clock)
        BookingListTab.Upcoming -> BookingsDateRangePresets.upcoming(clock)
        BookingListTab.All -> BookingsFilterOption.DateRange.DEFAULT
    }
}
