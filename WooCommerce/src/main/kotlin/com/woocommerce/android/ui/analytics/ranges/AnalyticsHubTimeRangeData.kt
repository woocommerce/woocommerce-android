package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.util.DateUtils
import java.util.*

abstract class AnalyticsHubTimeRangeData(
    dateUtils: DateUtils
) {
    abstract val currentRange: AnalyticsHubTimeRange
    abstract val previousRange: AnalyticsHubTimeRange
}
