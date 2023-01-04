package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentQuarter
import com.woocommerce.android.extensions.oneQuarterAgo
import com.woocommerce.android.extensions.startOfCurrentQuarter
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class AnalyticsHubLastQuarterRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneQuarterAgo = referenceDate.oneQuarterAgo(calendar)
        calendar.time = oneQuarterAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = calendar.endOfCurrentQuarter()
        )

        val twoQuartersAgo = oneQuarterAgo.oneQuarterAgo(calendar)
        calendar.time = twoQuartersAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentQuarter(),
            end = calendar.endOfCurrentQuarter()
        )
    }
}
