package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import com.woocommerce.commons.extensions.endOfCurrentDay
import com.woocommerce.commons.extensions.formatToMMMddYYYY
import com.woocommerce.commons.extensions.oneDayAgo
import com.woocommerce.commons.extensions.startOfCurrentDay
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils

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
    selectedSite: SiteModel,
    locale: Locale,
    referenceCalendar: Calendar
) {
    val currentRange: StatsTimeRange
    val previousRange: StatsTimeRange
    val formattedCurrentRange: String
    val formattedPreviousRange: String

    init {
        val referenceDate = generateCurrentDateInSiteTimeZone(selectedSite, locale)
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



    private fun generateCurrentDateInSiteTimeZone(
        selectedSite: SiteModel,
        locale: Locale
    ): Date {
        val targetTimezone = SiteUtils.getNormalizedTimezone(selectedSite.timezone).toZoneId()
        val currentDateTime = LocalDateTime.now()
        val zonedDateTime = ZonedDateTime.of(currentDateTime, ZoneId.systemDefault())
            .withZoneSameInstant(targetTimezone)
        // Format the result as a string
        val currentDateString = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getDateFromFullDateString(currentDateString, locale) ?: Date()
    }

    private fun getDateFromFullDateString(
        isoStringDate: String,
        locale: Locale
    ): Date? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
            formatter.parse(isoStringDate)
        } catch (e: Exception) {
            null
        }
    }
}
