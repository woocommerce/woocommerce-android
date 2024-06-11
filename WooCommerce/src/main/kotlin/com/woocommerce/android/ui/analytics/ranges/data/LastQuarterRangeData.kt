package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentQuarter
import com.woocommerce.android.extensions.formatAsRangeWith
import com.woocommerce.android.extensions.oneQuarterAgo
import com.woocommerce.android.extensions.startOfCurrentQuarter
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeData
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Responsible for defining two ranges of data, the current one, starting from the first day of the last quarter
// until the final day of that same quarter, and the previous one as two quarters ago, also starting
// from the first day until the final day of that quarter. E. g.
//
// Today: 29 Jul 2022
// When user requests report at 05:49 PM
// Current range: Apr 1, 00:00 until Jun 30, 23:59:59, 2022
// Previous range: Jan 1, 00:00 until Mar 31, 23:59:59, 2022
//
class LastQuarterRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val oneQuarterAgo = referenceDate.oneQuarterAgo()
        calendar.time = oneQuarterAgo
        val currentStart = calendar.startOfCurrentQuarter()
        val currentEnd = calendar.endOfCurrentQuarter()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(currentEnd, locale, calendar)

        val twoQuartersAgo = oneQuarterAgo.oneQuarterAgo()
        calendar.time = twoQuartersAgo
        val previousStart = calendar.startOfCurrentQuarter()
        val previousEnd = calendar.endOfCurrentQuarter()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = previousEnd
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(previousEnd, locale, calendar)
    }
}
