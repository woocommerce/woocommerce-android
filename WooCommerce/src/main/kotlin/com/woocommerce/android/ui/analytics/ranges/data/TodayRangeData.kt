package com.woocommerce.android.ui.analytics.ranges.data

import com.woocommerce.android.extensions.endOfCurrentDay
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.oneDayAgo
import com.woocommerce.android.extensions.startOfCurrentDay
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRange
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubTimeRangeData
import java.util.Calendar
import java.util.Date
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
    referenceDate: Date,
    locale: Locale,
    referenceCalendar: Calendar
) : AnalyticsHubTimeRangeData(referenceCalendar) {
    override val currentRange: AnalyticsHubTimeRange
    override val previousRange: AnalyticsHubTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        calendar.time = referenceDate
        currentRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = calendar.endOfCurrentDay()
        )
        formattedCurrentRange = referenceDate.formatToMMMddYYYY(locale)

        val yesterday = referenceDate.oneDayAgo()
        calendar.time = yesterday
        previousRange = AnalyticsHubTimeRange(
            start = calendar.startOfCurrentDay(),
            end = yesterday
        )
        formattedPreviousRange = yesterday.formatToMMMddYYYY(locale)
    }
}
