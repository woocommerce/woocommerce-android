package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentQuarter
import com.woocommerce.android.extensions.oneQuarterAgo
import com.woocommerce.android.extensions.startOfCurrentQuarter
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, the current one, starting from the first day of the last quarter
// until the final day of that same quarter, and the previous one as two quarters ago, also starting
// from the first day until the final day of that quarter. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Apr 1, 00:00 until Jun 30, 23:59:59, 2022
// Previous range: Jan 1, 00:00 until Mar 31, 23:59:59, 2022
//
class LastQuarterRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneQuarterAgo = referenceDate.oneQuarterAgo()
        calendar.time = oneQuarterAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = calendar.endOfCurrentQuarter()
        )

        val twoQuartersAgo = oneQuarterAgo.oneQuarterAgo()
        calendar.time = twoQuartersAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = calendar.endOfCurrentQuarter()
        )
    }
}
