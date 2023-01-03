package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentWeek
import com.woocommerce.android.extensions.oneWeekAgo
import com.woocommerce.android.extensions.startOfCurrentWeek
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, one starting from the first day of the last week
// until the final day of that week, and the previous one as two weeks ago, also starting
// from the first day until the final day of that week. E. g.
//
// Today: 29 Jul 2022
// Current range: Jul 18 until Jul 24, 2022
// Previous range: Jul 11 until Jul 17, 2022
//
class AnalyticsHubLastWeekRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneWeekAgo = referenceDate.oneWeekAgo()
        calendar.time = oneWeekAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentWeek(),
            end = calendar.endOfCurrentWeek()
        )

        val twoWeeksAgo = oneWeekAgo.oneWeekAgo()
        calendar.time = twoWeeksAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentWeek(),
            end = calendar.endOfCurrentWeek()
        )
    }
}
