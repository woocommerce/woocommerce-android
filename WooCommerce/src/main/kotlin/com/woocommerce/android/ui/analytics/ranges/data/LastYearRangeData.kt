package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentYear
import com.woocommerce.android.extensions.oneYearAgo
import com.woocommerce.android.extensions.startOfCurrentYear
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class LastYearRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneYearAgo = referenceDate.oneYearAgo(calendar)
        calendar.time = oneYearAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = calendar.endOfCurrentYear()
        )

        val twoYearsAgo = oneYearAgo.oneYearAgo(calendar)
        calendar.time = twoYearsAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentYear(),
            end = calendar.endOfCurrentYear()
        )
    }
}
