package com.woocommerce.android.ui.analytics.ranges

import java.util.Calendar

abstract class AnalyticsHubTimeRangeData(
    referenceCalendar: Calendar
) {
    abstract val currentRange: AnalyticsHubTimeRange
    abstract val previousRange: AnalyticsHubTimeRange

    protected val calendar = referenceCalendar.clone() as Calendar
}
