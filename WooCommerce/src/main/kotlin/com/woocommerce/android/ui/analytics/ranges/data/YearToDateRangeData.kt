package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneYearAgo
import com.woocommerce.android.extensions.startOfCurrentYear
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

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
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = referenceDate
        )

        val oneYearAgo = referenceDate.oneYearAgo(calendar)
        calendar.time = oneYearAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = oneYearAgo
        )
    }
}
