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

// Responsible for defining two ranges of data, the current one, starting from the first day of the current week
// until the current date, and the previous one, starting from the first day of the previous week
// until the same day of that week. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 25, 00:00 until Jul 29, 05:49 PM, 2022
// Previous range: Jul 18, 00:00 until Jul 22, 05:49 PM, 2022
//
class WeekToDateRangeData(
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
        val currentStart = calendar.startOfCurrentWeek()
        val currentEnd = calendar.endOfCurrentWeek()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(referenceDate, locale, calendar)

        val oneWeekAgo = referenceDate.oneWeekAgo()
        calendar.time = oneWeekAgo
        val startOfPreviousWeek = calendar.startOfCurrentWeek()
        previousRange = StatsTimeRange(
            start = startOfPreviousWeek,
            end = oneWeekAgo
        )
        formattedPreviousRange = startOfPreviousWeek.formatAsRangeWith(oneWeekAgo, locale, calendar)
    }
}
