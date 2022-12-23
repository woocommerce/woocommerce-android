package com.woocommerce.android.ui.analytics.ranges

import com.woocommerce.android.extensions.formatToDayMonth
import com.woocommerce.android.extensions.formatToDayMonthYear
import com.woocommerce.android.extensions.formatToDayYear
import com.woocommerce.android.extensions.isInSameMonthAs
import com.woocommerce.android.extensions.isInSameYearAs
import java.io.Serializable
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
): Serializable {
    val description: String
        get() = "$start - $end"

    fun generateDescription(simplified: Boolean, locale: Locale, calendar: Calendar): String {
        if (simplified) {
            return start.formatToDayMonthYear(locale)
        }

        val formattedStartDate = if (start.isInSameYearAs(end, calendar)) {
            start.formatToDayMonth(locale)
        } else {
            start.formatToDayMonthYear(locale)
        }

        val formattedEndDate = if (start.isInSameMonthAs(end, calendar)) {
            end.formatToDayYear(locale)
        } else {
            end.formatToDayMonthYear(locale)
        }

        return "$formattedStartDate - $formattedEndDate"
    }
}
