package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentYear
import com.woocommerce.android.extensions.formatAsRangeWith
import com.woocommerce.android.extensions.oneYearAgo
import com.woocommerce.android.extensions.startOfCurrentYear
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeData
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from January 1st  of the current year
// until the current date and the previous one, starting from January 1st of the last year
// until the same day on the in that year. E. g.
//
// Today: 1 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jan 1, 00:00 until Jul 1, 05:49 PM, 2022
// Previous range: Jan 1, 00:00 until Jul 1, 05:49 PM, 2021
//
class YearToDateRangeData(
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
        val currentStart = calendar.startOfCurrentYear()
        val currentEnd = calendar.endOfCurrentYear()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(referenceDate, locale, calendar)

        val oneYearAgo = referenceDate.oneYearAgo()
        calendar.time = oneYearAgo
        val previousStart = calendar.startOfCurrentYear()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = oneYearAgo
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(oneYearAgo, locale, calendar)
    }
}
