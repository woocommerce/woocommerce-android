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

// Responsible for defining two ranges of data, the current one, starting from January 1st of the last year
// until December 31th of that same year, and the previous one as two years ago, also ranging
// all days of that year. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jan 1, 00:00 until Dec 31, 23:59:59, 2021
// Previous range: Jan 1, 00:00 until Dec 31, 23:59:59, 2020
//
class LastYearRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val oneYearAgo = referenceDate.oneYearAgo()
        calendar.time = oneYearAgo
        val currentStart = calendar.startOfCurrentYear()
        val currentEnd = calendar.endOfCurrentYear()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(currentEnd, locale, calendar)

        val twoYearsAgo = oneYearAgo.oneYearAgo()
        calendar.time = twoYearsAgo
        val previousStart = calendar.startOfCurrentYear()
        val previousEnd = calendar.endOfCurrentYear()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = previousEnd
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(previousEnd, locale, calendar)
    }
}
