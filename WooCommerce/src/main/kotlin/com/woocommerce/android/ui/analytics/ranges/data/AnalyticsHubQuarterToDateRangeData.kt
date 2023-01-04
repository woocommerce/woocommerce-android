package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneQuarterAgo
import com.woocommerce.android.extensions.startOfCurrentQuarter
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubQuarterToDateRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = referenceDate
        )

        val oneQuarterAgo = referenceDate.oneQuarterAgo(calendar)
        calendar.time = oneQuarterAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = oneQuarterAgo
        )
    }
}
