package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.*

class AnalyticsHubTodayRangeData(
    referenceDate: Date,
    calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange

    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = referenceDate
        )

        val yesterday = referenceDate.oneDayAgo(calendar)
        calendar.time = yesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = yesterday
        )
    }
}
