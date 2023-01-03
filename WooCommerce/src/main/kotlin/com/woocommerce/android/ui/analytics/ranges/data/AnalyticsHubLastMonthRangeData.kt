package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentMonth
import com.woocommerce.android.extensions.oneMonthAgo
import com.woocommerce.android.extensions.startOfCurrentMonth
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, one starting from the first day of the last month
// until the final day of that same month, and the previous one as two months ago, also starting
// from the first day until the final day of that month. E. g.
//
// Today: 29 Jul 2022
// Current range: Jun 1 until Jun 30, 2022
// Previous range: May 1 until May 31, 2022
//
class AnalyticsHubLastMonthRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneMonthAgo = referenceDate.oneMonthAgo()
        calendar.time = oneMonthAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = calendar.endOfCurrentMonth()
        )

        val twoMonthsAgo = oneMonthAgo.oneMonthAgo()
        calendar.time = twoMonthsAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = calendar.endOfCurrentMonth()
        )
    }
}
