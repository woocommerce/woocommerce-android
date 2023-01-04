package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentMonth
import com.woocommerce.android.extensions.oneMonthAgo
import com.woocommerce.android.extensions.startOfCurrentMonth
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class LastMonthRangeData(
    referenceDate: Date,
    calendar: Calendar
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneMonthAgo = referenceDate.oneMonthAgo()
        calendar.time = oneMonthAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = calendar.endOfCurrentMonth()
        )

        val twoMonthsAgo = oneMonthAgo.oneMonthAgo()
        calendar.time = twoMonthsAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentMonth(),
            end = calendar.endOfCurrentMonth()
        )
    }
}
