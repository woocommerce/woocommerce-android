package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.formatToDayMonthYear
import java.io.Serializable
import java.util.Date
import java.util.Locale

data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
): Serializable {
    val description: String
        get() = "$start - $end"

    fun generateDescription(simplified: Boolean, locale: Locale): String {
        if (simplified) {
            return start.formatToDayMonthYear(locale)
        }



        return description
    }
}
