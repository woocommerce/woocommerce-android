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

// Responsible for defining two ranges of data, the current one, starting from the first day of the last month
// until the final day of that same month, and the previous one as two months ago, also starting
// from the first day until the final day of that month. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jun 1, 00:00 until Jun 30, 23:59:59, 2022
// Previous range: May 1, 00:00 until May 31, 23:59:59, 2022
//
class LastMonthRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val oneMonthAgo = referenceDate.oneMonthAgo()
        calendar.time = oneMonthAgo
        val currentStart = calendar.startOfCurrentMonth()
        val currentEnd = calendar.endOfCurrentMonth()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(currentEnd, locale, calendar)

        val twoMonthsAgo = oneMonthAgo.oneMonthAgo()
        calendar.time = twoMonthsAgo
        val previousStart = calendar.startOfCurrentMonth()
        val previousEnd = calendar.endOfCurrentMonth()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = previousEnd
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(previousEnd, locale, calendar)
    }
}
