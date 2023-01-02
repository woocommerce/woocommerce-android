package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubCustomRangeData(
    startDate: Date,
    endDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val dayDifference = Date(endDate.time - startDate.time)
        val previousEnd = startDate.oneDayAgo(calendar)
        val previousStart = Date(previousEnd.time - dayDifference.time)

        currentRange = AnalyticsHubTimeRange(
            start = startDate,
            end = endDate
        )

        previousRange = AnalyticsHubTimeRange(
            start = previousStart,
            end = previousEnd
        )
    }
}
