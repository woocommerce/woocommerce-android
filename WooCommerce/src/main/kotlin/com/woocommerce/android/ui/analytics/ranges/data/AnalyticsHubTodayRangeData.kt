package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.startOfToday
import com.woocommerce.android.extensions.theDayBeforeIt
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.*

class AnalyticsHubTodayRangeData(
    private val referenceDate: Date,
    private val calendar: Calendar
): AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
        get() = AnalyticsHubTimeRange(
            start = calendar.startOfToday(),
            end = referenceDate
        )

    override val previousRange: AnalyticsHubTimeRange
        get() {
            val yesterday = referenceDate.theDayBeforeIt()
            calendar.time = yesterday
            return AnalyticsHubTimeRange(
                start = calendar.startOfToday(),
                end = yesterday
            )
        }
}
