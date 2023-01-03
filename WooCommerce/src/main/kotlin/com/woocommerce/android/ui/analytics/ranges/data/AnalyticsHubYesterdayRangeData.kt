package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

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

        val dayBeforeYesterday = yesterday.oneDayAgo()
        calendar.time = dayBeforeYesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = calendar.endOfCurrentDay()
        )
    }
}
