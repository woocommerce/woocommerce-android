package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import com.woocommerce.commons.extensions.endOfCurrentDay
import com.woocommerce.commons.extensions.formatToMMMddYYYY
import com.woocommerce.commons.extensions.oneDayAgo
import com.woocommerce.commons.extensions.startOfCurrentDay
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.parcelize.Parcelize

// Responsible for defining two ranges of data, the current one, starting from the first second of the current day
// until the same day in the current timezone, and the previous one, starting from the first second of
// yesterday until the same time of that day. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 29, 00:00 until Jul 29, 05:49 PM, 2022
// Previous range: Jul 28, 00:00 until Jul 28, 05:49 PM, 2022
//

@Parcelize
data class StatsTimeRange(
    val start: Date,
    val end: Date
) : Parcelable

class TodayRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) {
    val currentRange: StatsTimeRange
    val previousRange: StatsTimeRange
    val formattedCurrentRange: String
    val formattedPreviousRange: String

    init {
        val calendar = referenceCalendar.clone() as Calendar
        calendar.time = referenceDate
        val currentStart = calendar.startOfCurrentDay()
        val currentEnd = calendar.endOfCurrentDay()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = referenceDate.formatToMMMddYYYY(locale)

        val yesterday = referenceDate.oneDayAgo()
        calendar.time = yesterday
        val previousStart = calendar.startOfCurrentDay()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = yesterday
        )
        formattedPreviousRange = yesterday.formatToMMMddYYYY(locale)
    }
}
