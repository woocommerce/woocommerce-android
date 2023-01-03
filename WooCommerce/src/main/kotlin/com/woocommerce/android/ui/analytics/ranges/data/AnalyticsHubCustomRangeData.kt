package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubCustomRangeData(
    selectedStartDate: Date,
    selectedEndDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = selectedStartDate
        val currentStart = calendar.startOfCurrentDay()

        calendar.time = selectedEndDate
        val currentEnd = calendar.endOfCurrentDay()

        currentRange = AnalyticsHubTimeRange(
            start = currentStart,
            end = currentEnd
        )

        val dayDifference = selectedEndDate.time - selectedStartDate.time

        calendar.time = selectedStartDate.oneDayAgo()
        val previousEnd = calendar.endOfCurrentDay()

        calendar.time = Date(previousEnd.time - dayDifference)
        val previousStart = calendar.startOfCurrentDay()

        previousRange = AnalyticsHubTimeRange(
            start = previousStart,
            end = previousEnd
        )
    }
}
