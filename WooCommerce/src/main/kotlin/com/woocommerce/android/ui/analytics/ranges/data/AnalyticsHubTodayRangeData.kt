package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.extensions.theDayBeforeIt
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date

// Responsible for defining two ranges of data, one starting from the the first second of the current day
// until the same day in the current time and the previous one, starting from the first second of
// yesterday until the same time of that day. E. g.
//
// Today: 29 Jul 2022
// Current range: Jul 29 until Jul 29, 2022
// Previous range: Jul 28 until Jul 28, 2022
//
class AnalyticsHubTodayRangeData(
    referenceDate: Date,
    calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange

    override val previousRange: AnalyticsHubTimeRange

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = referenceDate
        )

        val yesterday = referenceDate.theDayBeforeIt(calendar)
        calendar.time = yesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = yesterday
        )
    }
}
