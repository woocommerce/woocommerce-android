package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.oneWeekAgo
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import com.woocommerce.android.util.DateUtils
import java.util.*

class AnalyticsHubWeekToDateRangeData(
    referenceDate: Date,
    calendar: Calendar,
    dateUtils: DateUtils
): AnalyticsHubTimeRangeData(dateUtils) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        val startOfCurrentWeek = dateUtils.getDateForFirstDayOfWeek(calendar)
        currentRange = AnalyticsHubTimeRange(
            start = startOfCurrentWeek,
            end = referenceDate
        )

        val oneWeekAgo = referenceDate.oneWeekAgo(calendar)
        calendar.time = oneWeekAgo
        val startOfPreviousWeek = dateUtils.getDateForFirstDayOfWeek(calendar)
        previousRange = AnalyticsHubTimeRange(
            start = startOfPreviousWeek,
            end = oneWeekAgo
        )
    }
}
