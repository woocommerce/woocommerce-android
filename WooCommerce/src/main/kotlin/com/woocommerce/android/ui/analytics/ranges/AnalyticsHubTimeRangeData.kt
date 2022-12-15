package com.woocommerce.android.ui.analytics.ranges

import java.util.*

abstract class AnalyticsHubTimeRangeData {
    abstract val currentRangeStart: Date
    abstract val currentRangeEnd: Date
    abstract val previousRangeStart: Date
    abstract val previousRangeEnd: Date

    val currentRange: AnalyticsHubTimeRange
        get() = AnalyticsHubTimeRange(currentRangeStart, currentRangeEnd)

    val previousRange: AnalyticsHubTimeRange
        get() = AnalyticsHubTimeRange(previousRangeStart, previousRangeEnd)
}
