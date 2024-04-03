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

// Responsible for defining two ranges of data, the current one, starting from the first day of the current quarter
// until the current date and the previous one, starting from the first day of the previous quarter
// until the same relative day of the previous quarter. E. g.
//
// Today: 15 Feb 2022
// When user requests report at 05:49 PM
// Current range: Jan 1, 00:00 until Feb 15, 05:49 PM, 2022
// Previous range: Oct 1, 00:00 until Nov 15, 05:49 PM, 2021
//
class QuarterToDateRangeData(
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        calendar.time = referenceDate
        val currentStart = calendar.startOfCurrentQuarter()
        val currentEnd = calendar.endOfCurrentQuarter()
        currentRange = StatsTimeRange(
            start = currentStart,
            end = currentEnd
        )
        formattedCurrentRange = currentStart.formatAsRangeWith(referenceDate, locale, calendar)

        val oneQuarterAgo = referenceDate.oneQuarterAgo()
        calendar.time = oneQuarterAgo
        val previousStart = calendar.startOfCurrentQuarter()
        previousRange = StatsTimeRange(
            start = previousStart,
            end = oneQuarterAgo
        )
        formattedPreviousRange = previousStart.formatAsRangeWith(oneQuarterAgo, locale, calendar)
    }
}
