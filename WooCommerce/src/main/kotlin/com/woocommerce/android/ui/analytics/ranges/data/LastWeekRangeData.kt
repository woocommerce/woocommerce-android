package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentWeek
import com.woocommerce.android.extensions.formatAsRangeWith
import com.woocommerce.android.extensions.oneWeekAgo
import com.woocommerce.android.extensions.startOfCurrentWeek
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeData
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from the first day of the last week
// until the final day of that week, and the previous one as two weeks ago, also starting
// from the first day until the final day of that week. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 18, 00:00 until Jul 24, 23:59:59, 2022
// Previous range: Jul 11, 00:00 until Jul 17, 23:59:59, 2022
//
class LastWeekRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val oneWeekAgo = referenceDate.oneWeekAgo()
        calendar.time = oneWeekAgo
        val currentStart = calendar.startOfCurrentWeek()
        val currentEnd = calendar.endOfCurrentWeek()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(currentEnd, locale, calendar)

        val twoWeeksAgo = oneWeekAgo.oneWeekAgo()
        calendar.time = twoWeeksAgo
        val previousStart = calendar.startOfCurrentWeek()
        val previousEnd = calendar.endOfCurrentWeek()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = previousEnd
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(previousEnd, locale, calendar)
    }
}
