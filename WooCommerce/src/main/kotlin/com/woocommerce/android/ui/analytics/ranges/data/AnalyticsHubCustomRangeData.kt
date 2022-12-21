package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Date

class AnalyticsHubCustomRangeData(
    val startDate: Date,
    val endDate: Date
) : AnalyticsHubTimeRangeData {
    override val currentRange: AnalyticsHubTimeRange
        get() = AnalyticsHubTimeRange(startDate, endDate)

    override val previousRange: AnalyticsHubTimeRange
        get() = AnalyticsHubTimeRange(startDate, endDate)
}
