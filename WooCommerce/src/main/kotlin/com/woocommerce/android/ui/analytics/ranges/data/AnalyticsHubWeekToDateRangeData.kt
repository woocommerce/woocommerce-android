package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.*

class AnalyticsHubWeekToDateRangeData(
    currentDate: Date,
    calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        currentRange = AnalyticsHubTimeRange(
            start = currentDate,
            end = currentDate
        )

        previousRange = AnalyticsHubTimeRange(
            start = currentDate,
            end = currentDate
        )
    }
}
