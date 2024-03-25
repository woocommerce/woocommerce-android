package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentMonth
import com.woocommerce.android.extensions.formatAsRangeWith
import com.woocommerce.android.extensions.oneMonthAgo
import com.woocommerce.android.extensions.startOfCurrentMonth
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeData
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from the first day of the current month
// until the current date, and the previous one, starting from the first day of the previous month
// until the same day of the previous month. E. g.
//
// Today: 31 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 1, 00:00 until Jul 31, 05:49 PM, 2022
// Previous range: Jun 1, 00:00 until Jun 30, 05:49 PM, 2022
//
class MonthToDateRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        calendar.time = referenceDate
        val currentStart = calendar.startOfCurrentMonth()
        val currentEnd = calendar.endOfCurrentMonth()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(referenceDate, locale, calendar)

        val oneMonthAgo = referenceDate.oneMonthAgo()
        calendar.time = oneMonthAgo
        val previousStart = calendar.startOfCurrentMonth()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = oneMonthAgo
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(oneMonthAgo, locale, calendar)
    }
}
