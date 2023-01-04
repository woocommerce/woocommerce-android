package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneWeekAgo
import com.woocommerce.android.extensions.startOfCurrentWeek
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class WeekToDateRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        val startOfCurrentWeek = calendar.startOfCurrentWeek()
        currentRange = AnalyticsHubTimeRange(
            start = startOfCurrentWeek,
            end = referenceDate
        )

        val oneWeekAgo = referenceDate.oneWeekAgo()
        calendar.time = oneWeekAgo
        val startOfPreviousWeek = calendar.startOfCurrentWeek()
        previousRange = AnalyticsHubTimeRange(
            start = startOfPreviousWeek,
            end = oneWeekAgo
        )
    }
}
