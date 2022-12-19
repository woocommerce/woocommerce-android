package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.extensions.theDayBeforeIt
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.*

class AnalyticsHubYesterdayRangeData(
    referenceDate: Date,
    calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange

    init {
        val yesterday = referenceDate.theDayBeforeIt()
        calendar.time = yesterday
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = yesterday
        )

        val dayBeforeYesterday = yesterday.theDayBeforeIt(calendar)
        calendar.time = dayBeforeYesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = dayBeforeYesterday
        )
    }
}
