package com.woocommerce.android.ui.analytics.ranges

import android.os.Parcelable
import com.woocommerce.android.extensions.formatToDDyyyy
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.isInSameMonthAs
import com.woocommerce.android.extensions.isInSameYearAs
import kotlinx.parcelize.Parcelize
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Parcelize
data class AnalyticsHubTimeRange(
    val start: Date,
    val end: Date
) : Parcelable {
    fun generateDescription(simplified: Boolean, locale: Locale, calendar: Calendar): String {
        if (simplified) {
            return start.formatToMMMddYYYY(locale)
        }

        val formattedStartDate = if (start.isInSameYearAs(end, calendar)) {
            start.formatToMMMdd(locale)
        } else {
            start.formatToMMMddYYYY(locale)
        }

        val formattedEndDate = if (start.isInSameMonthAs(end, calendar)) {
            end.formatToDDyyyy(locale)
        } else {
            end.formatToMMMddYYYY(locale)
        }

        return "$formattedStartDate - $formattedEndDate"
    }

    companion object {
        val EMPTY = AnalyticsHubTimeRange(Date(), Date())
    }
}
