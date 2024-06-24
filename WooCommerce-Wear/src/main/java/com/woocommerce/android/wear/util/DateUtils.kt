package com.woocommerce.android.wear.util

import com.woocommerce.android.wear.ui.login.LoginRepository
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import javax.inject.Inject

class DateUtils @Inject constructor(
    private val loginRepository: LoginRepository,
    private val locale: Locale
) {
    private val selectedSite get() = loginRepository.selectedSiteFlow.value
    private val yyyyMMddFormat = SimpleDateFormat("yyyy-MM-dd", locale)
    private val friendlyMonthDayFormat = SimpleDateFormat("MMM d", locale)
    private val friendlyMonthDayYearFormat = SimpleDateFormat("MMM d, yyyy", locale)

    fun getFormattedDateWithSiteTimeZone(dateCreated: String): String? {
        val currentSiteDate = getCurrentDateInSiteTimeZone() ?: Date()
        val siteDate = getDateUsingSiteTimeZone(dateCreated) ?: Date()
        val iso8601DateString = getYearMonthDayStringFromDate(siteDate)

        return if (DateTimeUtils.isSameYear(currentSiteDate, siteDate)) {
            getShortMonthDayString(iso8601DateString)
        } else {
            getShortMonthDayAndYearString(iso8601DateString)
        }
    }

    fun generateCurrentDateInSiteTimeZone(): Date {
        val site = selectedSite ?: return Date()
        val targetTimezone = SiteUtils.getNormalizedTimezone(site.timezone).toZoneId()
        val currentDateTime = LocalDateTime.now()
        val zonedDateTime = ZonedDateTime.of(currentDateTime, ZoneId.systemDefault())
            .withZoneSameInstant(targetTimezone)
        // Format the result as a string
        val currentDateString = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getDateFromFullDateString(currentDateString) ?: Date()
    }

    fun getCurrentDateInSiteTimeZone(): Date? {
        val site = selectedSite ?: return null
        val targetTimezone = SiteUtils.getNormalizedTimezone(site.timezone).toZoneId()
        val currentDateTime = LocalDateTime.now()
        val zonedDateTime = ZonedDateTime.of(currentDateTime, ZoneId.systemDefault())
            .withZoneSameInstant(targetTimezone)
        // Format the result as a string
        val currentDateString = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getDateFromFullDateString(currentDateString)
    }

    fun getDateUsingSiteTimeZone(isoStringDate: String): Date? {
        if (isoStringDate.isEmpty()) return null
        val iso8601DateString = iso8601OnSiteTimeZoneFromIso8601UTC(isoStringDate)
        return getDateFromFullDateString(iso8601DateString)
    }

    fun getYearMonthDayStringFromDate(date: Date): String = yyyyMMddFormat.format(date)

    fun getShortMonthDayString(iso8601Date: String): String? {
        return runCatching {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyMonthDayFormat.format(date)
        }.getOrNull()
    }

    fun getShortMonthDayAndYearString(iso8601Date: String): String? {
        return runCatching {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyMonthDayYearFormat.format(date)
        }.getOrNull()
    }

    @Suppress("SwallowedException")
    private fun iso8601OnSiteTimeZoneFromIso8601UTC(iso8601date: String): String {
        return runCatching {
            val site = selectedSite ?: return iso8601date
            // Parse ISO 8601 string to LocalDateTime object
            val utcDateTime = LocalDateTime.parse(iso8601date, DateTimeFormatter.ISO_DATE_TIME)

            // Specify the target timezone
            val targetTimezone = SiteUtils.getNormalizedTimezone(site.timezone).toZoneId()

            val zonedDateTime = ZonedDateTime.of(utcDateTime, ZoneId.of("UTC"))
                .withZoneSameInstant(targetTimezone)

            // Format the result as a string
            zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }.getOrNull() ?: iso8601date
    }

    private fun getDateFromFullDateString(isoStringDate: String): Date? {
        return runCatching {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
            formatter.parse(isoStringDate)
        }.getOrNull()
    }
}
