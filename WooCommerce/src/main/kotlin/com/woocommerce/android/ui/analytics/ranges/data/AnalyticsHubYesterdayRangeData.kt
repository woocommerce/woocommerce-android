package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, one starting from the the first second of yesterday
// until the last minute of  the same day and the previous one, starting from the first second of
// the day before yesterday until the end of that day. E. g.
//
// Today: 29 Jul 2022
// Current range: Jul 28 until Jul 28, 2022
// Previous range: Jul 27 until Jul 27, 2022
//
class AnalyticsHubYesterdayRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val yesterday = referenceDate.oneDayAgo()
        calendar.time = yesterday
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = calendar.endOfCurrentDay()
        )

        val dayBeforeYesterday = yesterday.oneDayAgo(calendar)
        calendar.time = dayBeforeYesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = calendar.endOfCurrentDay()
        )
    }
}
