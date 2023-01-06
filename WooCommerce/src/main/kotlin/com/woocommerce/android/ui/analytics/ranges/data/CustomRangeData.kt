package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data based on user provided dates.
// The current range will be what user provided.
// The previous range will be a range of the same length ending on the day before the current range starts.
//
// Current range: Jan 5, 00:00 - Jan 7, 23:59:59, 2022
// Previous range: Jan 2, 00:00 - Jan 4, 23:59:59, 2022
//
class CustomRangeData(
    selectedStartDate: Date,
    selectedEndDate: Date,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
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
