package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentWeek
import com.woocommerce.android.extensions.oneWeekAgo
import com.woocommerce.android.extensions.startOfCurrentWeek
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

class LastWeekRangeData(
    referenceDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val oneWeekAgo = referenceDate.oneWeekAgo()
        calendar.time = oneWeekAgo
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentWeek(),
            end = calendar.endOfCurrentWeek()
        )

        val twoWeeksAgo = oneWeekAgo.oneWeekAgo()
        calendar.time = twoWeeksAgo
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentWeek(),
            end = calendar.endOfCurrentWeek()
        )
    }
}
