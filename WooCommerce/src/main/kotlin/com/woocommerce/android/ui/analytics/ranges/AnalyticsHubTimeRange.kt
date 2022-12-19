package com.woocommerce.android.ui.analytics.ranges

import java.util.Date

data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
) {
    val description: String
        get() = "$start - $end"
}
