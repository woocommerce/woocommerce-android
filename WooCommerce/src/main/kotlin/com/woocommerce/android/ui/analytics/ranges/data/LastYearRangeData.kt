package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentYear
import com.woocommerce.android.extensions.oneYearAgo
import com.woocommerce.android.extensions.startOfCurrentYear
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

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
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneYearAgo = referenceDate.oneYearAgo()
        calendar.time = oneYearAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = calendar.endOfCurrentYear()
        )

        val twoYearsAgo = oneYearAgo.oneYearAgo()
        calendar.time = twoYearsAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = calendar.endOfCurrentYear()
        )
    }
}
