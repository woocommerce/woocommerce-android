package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneQuarterAgo
import com.woocommerce.android.extensions.startOfCurrentQuarter
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, the current one, starting from the first day of the current quarter
// until the current date and the previous one, starting from the first day of the previous quarter
// until the same relative day of the previous quarter. E. g.
//
// Today: 15 Feb 2022
// When user requests report at 05:49 PM
// Current range: Jan 1, 00:00 until Feb 15, 05:49 PM, 2022
// Previous range: Oct 1, 00:00 until Nov 15, 05:49 PM, 2021
//
class QuarterToDateRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = referenceDate
        )

        val oneQuarterAgo = referenceDate.oneQuarterAgo()
        calendar.time = oneQuarterAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = oneQuarterAgo
        )
    }
}
