package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentMonth
import com.woocommerce.android.extensions.oneMonthAgo
import com.woocommerce.android.extensions.startOfCurrentMonth
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, the current one, starting from the first day of the current month
// until the current date, and the previous one, starting from the first day of the previous month
// until the same day of the previous month. E. g.
//
// Today: 31 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 1, 00:00 until Jul 31, 05:49 PM, 2022
// Previous range: Jun 1, 00:00 until Jun 30, 05:49 PM, 2022
//
class MonthToDateRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = calendar.endOfCurrentMonth()
        )

        val oneMonthAgo = referenceDate.oneMonthAgo()
        calendar.time = oneMonthAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = oneMonthAgo
        )
    }
}
