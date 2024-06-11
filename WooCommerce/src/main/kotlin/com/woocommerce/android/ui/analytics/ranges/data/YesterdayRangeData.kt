package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeData
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from the first second of yesterday
// until the last minute of the same day, and the previous one, starting from the first second of
// the day before yesterday until the end of that same day. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 28, 00:00 until Jul 28, 23:59:59, 2022
// Previous range: Jul 27, 00:00 until Jul 27, 23:59:59, 2022
//
class YesterdayRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val yesterday = referenceDate.oneDayAgo()
        calendar.time = yesterday
        val currentStart = calendar.startOfCurrentDay()
        val currentEnd = calendar.endOfCurrentDay()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = yesterday.formatToMMMddYYYY(locale)

        val dayBeforeYesterday = yesterday.oneDayAgo()
        calendar.time = dayBeforeYesterday
        val previousStart = calendar.startOfCurrentDay()
        val previousEnd = calendar.endOfCurrentDay()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = previousEnd
        )
        formattedPreviousRange = dayBeforeYesterday.formatToMMMddYYYY(locale)
    }
}
