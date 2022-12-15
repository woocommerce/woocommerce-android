package com.woocommerce.android.ui.analytics.ranges

import java.util.*

abstract class AnalyticsHubTimeRangeData {
    abstract val currentRangeStart: Date
    abstract val currentRangeEnd: Date
    abstract val previousRangeStart: Date
    abstract val previousRangeEnd: Date
}
