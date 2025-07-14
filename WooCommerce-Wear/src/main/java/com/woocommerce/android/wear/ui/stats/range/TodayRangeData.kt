package com.woocommerce.android.wear.ui.stats.range

import com.woocommerce.android.wear.extensions.endOfCurrentDay
import com.woocommerce.android.wear.extensions.formatToMMMddYYYY
import com.woocommerce.android.wear.extensions.oneDayAgo
import com.woocommerce.android.wear.extensions.startOfCurrentDay
import com.woocommerce.android.wear.util.DateUtils
import java.util.Calendar
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from the first second of the current day
// until the same day in the current timezone, and the previous one, starting from the first second of
// yesterday until the same time of that day. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Jul 29, 00:00 until Jul 29, 05:49 PM, 2022
// Previous range: Jul 28, 00:00 until Jul 28, 05:49 PM, 2022
//

class TodayRangeData(
    dateUtils: DateUtils,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val referenceDate = dateUtils.generateCurrentDateInSiteTimeZone()
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
