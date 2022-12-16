package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.extensions.theDayBeforeIt
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.*

class AnalyticsHubYesterdayRangeData(
    private val referenceDate: Date,
    private val calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
        get() {
            val yesterday = referenceDate.theDayBeforeIt()
            calendar.time = yesterday
            return AnalyticsHubTimeRange(
                start = calendar.startOfCurrentDay(),
                end = yesterday
            )
        }
    override val previousRange: AnalyticsHubTimeRange
        get() = TODO("Not yet implemented")
}
