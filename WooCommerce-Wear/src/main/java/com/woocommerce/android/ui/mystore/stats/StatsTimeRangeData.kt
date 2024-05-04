package com.woocommerce.android.ui.mystore.stats

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Parcelize
data class StatsTimeRange(
    val start: Date,
    val end: Date
) : Parcelable

abstract class StatsTimeRangeData(
    referenceCalendar: Calendar
) {
    abstract val currentRange: StatsTimeRange
    abstract val previousRange: StatsTimeRange
    abstract val formattedCurrentRange: String
    abstract val formattedPreviousRange: String

    protected val calendar = referenceCalendar.clone() as Calendar

    protected fun generateCurrentDateInSiteTimeZone(
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
    ) = runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
        formatter.parse(isoStringDate)
    }.getOrNull()
}
