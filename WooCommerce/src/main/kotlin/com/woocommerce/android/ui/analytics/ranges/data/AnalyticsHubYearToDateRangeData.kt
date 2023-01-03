package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneYearAgo
import com.woocommerce.android.extensions.startOfCurrentYear
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubYearToDateRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = referenceDate
        )

        val oneYearAgo = referenceDate.oneYearAgo()
        calendar.time = oneYearAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = oneYearAgo
        )
    }
}
