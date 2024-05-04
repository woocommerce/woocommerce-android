package com.woocommerce.android.ui.mystore.stats

import com.woocommerce.commons.extensions.endOfCurrentDay
import com.woocommerce.commons.extensions.formatToMMMddYYYY
import com.woocommerce.commons.extensions.oneDayAgo
import com.woocommerce.commons.extensions.startOfCurrentDay
import org.wordpress.android.fluxc.model.SiteModel
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
    selectedSite: SiteModel,
    locale: Locale,
    referenceCalendar: Calendar
) : StatsTimeRangeData(referenceCalendar) {
    override val currentRange: StatsTimeRange
    override val previousRange: StatsTimeRange
    override val formattedCurrentRange: String
    override val formattedPreviousRange: String

    init {
        val referenceDate = generateCurrentDateInSiteTimeZone(selectedSite, locale)
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
