package com.woocommerce.android.ui.analytics.ranges

import java.util.Calendar

abstract class StatsTimeRangeData(
    referenceCalendar: Calendar
) {
    abstract val currentRange: AnalyticsHubTimeRange
    abstract val previousRange: AnalyticsHubTimeRange
    abstract val formattedCurrentRange: String
    abstract val formattedPreviousRange: String

    protected val calendar = referenceCalendar.clone() as Calendar
}
