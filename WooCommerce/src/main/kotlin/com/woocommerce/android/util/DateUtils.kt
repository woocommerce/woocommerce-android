package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.extensions.formatToEEEEMMMddhha
import com.woocommerce.android.extensions.formatToYYYYmm
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog.T.UTILS
import org.apache.commons.lang3.time.DateUtils
import org.wordpress.android.fluxc.utils.SiteUtils
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import javax.inject.Inject

class DateUtils @Inject constructor(
    private val locale: Locale,
    private val crashLogger: CrashLogging,
    private val selectedSite: SelectedSite
) {
    private val friendlyMonthDayFormat: SimpleDateFormat = SimpleDateFormat("MMM d", locale)
    private val friendlyMonthDayYearFormat: SimpleDateFormat = SimpleDateFormat("MMM d, yyyy", locale)
    private val friendlyTimeFormat: SimpleDateFormat = SimpleDateFormat("h:mm a", locale)
    private val friendlyLongMonthDayFormat: SimpleDateFormat = SimpleDateFormat("MMMM dd", locale)

    private val weekOfYearStartingMondayFormat: SimpleDateFormat = SimpleDateFormat("yyyy-ww", locale).apply {
        calendar = Calendar.getInstance().apply {
            // Ensure the date formatter follows ISO8601 week standards:
            // the first day of a week is a Monday, and the first week of the year starts on the first Monday
            // (and not on the Monday of the week containing January 1st, which may be in the previous year)
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 7
        }
    }

    private val shortMonths = DateFormatSymbols(locale).shortMonths

    private val yyyyMMddFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the number of days in the given month.
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getNumberOfDaysInMonth(iso8601Date: String): Int? {
        return try {
            val (year, month) = iso8601Date.split("-")
            // -1 because Calendar months are zero-based
            val calendar = GregorianCalendar(year.toInt(), month.toInt() - 1, 1)
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: IndexOutOfBoundsException) {
            "Date string argument is not of format YYYY-MM-DD: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD hh, returns the String in short month ("EEEE, MMM d") format.
     *
     * For example, given 2019-08-05 11 returns "Monday, Aug 5"
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getDayMonthDateString(iso8601Date: String): String? {
        return try {
            val targetFormat = SimpleDateFormat("EEEE, MMM d", locale)
            val (dateString, _) = iso8601Date.split(" ")
            val (year, month, day) = dateString.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            targetFormat.format(date)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD hh: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in short month ("MMM d") format.
     *
     * For example, given 2018-07-03 returns "Jul 3", and given 2018-07-28 returns "Jul 28".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortMonthDayString(iso8601Date: String): String? {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyMonthDayFormat.format(date)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in long month ("MMMM dd") format.
     *
     * For example, given 2018-07-03 returns "July 3", and given 2018-07-28 returns "July 28".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getLongMonthDayString(iso8601Date: String): String? {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyLongMonthDayFormat.format(date)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in YYYY-MM format.
     *
     * For example, given 2018-07-03 returns 2018-07, and given 2018-08-28 returns 2018-08.
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getYearMonthString(iso8601Date: String): String? {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            date.formatToYYYYmm(locale)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in short month ("MMM d, YYYY") format.
     *
     * For example, given 2018-07-03 returns "Jul 3, 2018", and given 2018-07-28 returns "Jul 28, 2018".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortMonthDayAndYearString(iso8601Date: String): String? {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyMonthDayYearFormat.format(date)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM-DD, returns the string in a localized full long date format.
     *
     * return null if the argument is not a valid YYYY-MM-DD date string.
     */
    fun getLocalizedLongDateString(context: Context, dateString: String): String? {
        return try {
            val (year, month, day) = dateString.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            DateFormat.getLongDateFormat(context).format(date)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM-DD: $dateString".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-WW, returns the String in short month ("MMM d") format,
     * with the day being the first day of that week (a Monday, by ISO8601 convention).
     *
     * For example, given 2018-W11, returns "Mar 12".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortMonthDayStringForWeek(iso8601Week: String): String? {
        return try {
            val date = weekOfYearStartingMondayFormat.parse(iso8601Week)
            friendlyMonthDayFormat.format(date!!)
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-WW: $iso8601Week".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM, returns the corresponding short month format.
     *
     * For example, given 2018-07, returns "Jul".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortMonthString(iso8601Month: String): String? {
        return try {
            val month = iso8601Month.split("-")[1]
            shortMonths[month.toInt() - 1]
        } catch (e: Exception) {
            "Date string argument is not of format YYYY-MM: $iso8601Month".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format MMMM d, YYYY, returns date string in yyyy-MM-dd format
     *
     * return null if the argument is not a valid date string.
     */
    fun getDateString(dateString: String): String? {
        return try {
            val originalFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ROOT)
            val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val date = originalFormat.parse(dateString)
            targetFormat.format(date!!)
        } catch (e: Exception) {
            "Date string argument is not of format MMMM dd, yyyy: $dateString".reportAsError(e)
            return null
        }
    }

    /**
     * Formats a date object and returns it in the format of yyyy-MM-dd
     */
    fun getYearMonthDayStringFromDate(date: Date): String = yyyyMMddFormat.format(date)

    /**
     * Given an ISO8601 date of format YYYY-MM-DD hh, returns the hour String in ("hh") format.
     *
     * For example, given 2019-07-15 13 returns "1pm", and given 2019-07-28 01 returns "1am".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortHourString(iso8601Date: String): String? {
        return try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH", locale)
            val targetFormat = SimpleDateFormat("hha", locale)
            val date = originalFormat.parse(iso8601Date)
            targetFormat.format(date!!).lowercase(locale).trimStart('0')
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM-dd H: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the day String in ("d") format.
     *
     * For example, given 2019-07-15 returns "15", and given 2019-07-28 returns "28".
     *
     * return null if the argument is not a valid iso8601 date string or not in the expected format.
     */
    fun getDayString(iso8601Date: String): String? {
        return try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd", locale)
            val targetFormat = SimpleDateFormat("d", locale)
            val date = originalFormat.parse(iso8601Date)
            targetFormat.format(date!!)
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM-dd: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD HH, returns the day String in ("EEEE, MMM dd › ha") format.
     *
     * For example, given 2023-12-27 12 returns "Wednesday, Dec 27 > 12 am"
     *
     * return null if the argument is not a valid iso8601 date string or not in the expected format.
     */
    fun getFriendlyDayHourString(iso8601Date: String): String? {
        return try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH", locale)
            val date = originalFormat.parse(iso8601Date)
            date!!.formatToEEEEMMMddhha(locale)
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM-dd HH: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM, returns the corresponding short month format.
     *
     * For example, given 2018-07, returns "Jul 2018".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getShortMonthYearString(iso8601Month: String): String? {
        return try {
            val (year, month) = iso8601Month.split("-")
            "${shortMonths[month.toInt() - 1]} $year"
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM: $iso8601Month".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM, returns the year month in (yyyy > MMMM) format.
     *
     * For example, given 2018-07, returns "2018 > July".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getFriendlyLongMonthYear(iso8601Month: String): String? {
        return try {
            val (year, month) = iso8601Month.split("-")
            "$year › ${DateFormatSymbols(locale).months[month.toInt() - 1]}"
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM: $iso8601Month".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM-dd, returns the corresponding full month format.
     *
     * For example, given 2018-07-02, returns "July".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    fun getMonthString(iso8601Date: String): String? {
        return try {
            val (_, month, _) = iso8601Date.split("-")
            DateFormatSymbols(locale).months[month.toInt() - 1]
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM-dd: $iso8601Date".reportAsError(e)
            return null
        }
    }

    /**
     * Given a date of format YYYY-MM, returns the corresponding year format.
     *
     * For example, given 2018-07, returns "2018".
     *
     * return null if the argument is not a valid iso8601 date string.
     */
    @Suppress("UNUSED_VARIABLE") // Applying the suggestion to rename 'month' to '_' makes the test fail
    fun getYearString(iso8601Month: String): String? {
        return try {
            val (year, month) = iso8601Month.split("-")
            year
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM: $iso8601Month".reportAsError(e)
            return null
        }
    }

    /**
     * Converts the given [year], [month], [day] to a [Date] object at midnight
     */
    fun getDateAtStartOfDay(
        year: Int,
        month: Int,
        day: Int
    ): Date {
        return GregorianCalendar(year, month, day).time
    }

    /**
     * Method to convert date string from MMM dd, yyyy format to yyyy-MM-dd format
     * i.e. Dec 02, 2020 is formatted to 2020-12-02
     */
    fun formatToYYYYmmDD(dateString: String): String? {
        return try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", locale)
            val date = dateFormat.parse(dateString) ?: Date()
            date.formatToYYYYmmDD()
        } catch (e: Exception) {
            "Date string argument is not of format MMM dd, yyyy: $dateString".reportAsError(e)
            return null
        }
    }

    private fun String.reportAsError(exception: Exception) {
        WooLog.e(UTILS, this)
        crashLogger.sendReport(exception = exception, message = this)
    }

    /**
     * Returns the same date received as parameter in millis at the end of the day: 23:59:59
     */
    fun getDateInMillisAtTheEndOfTheDay(dateMillis: Long): Long =
        Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = dateMillis
            set(Calendar.SECOND, getMaximum(Calendar.SECOND))
            set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
            set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
        }.timeInMillis

    /**
     * Returns the same date received as parameter in millis at the start of the day: 00:00:00
     */
    fun getDateInMillisAtTheStartOfTheDay(dateMillis: Long): Long =
        Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = dateMillis
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
        }.timeInMillis

    /**
     * Returns a date time in millis with the date for current day at 00:00:00
     */
    fun getDateForTodayAtTheStartOfTheDay(): Long =
        Calendar.getInstance().apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
        }.timeInMillis

    fun getCurrentDateTimeMinusDays(days: Int): Long =
        Calendar.getInstance().apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.DATE, -days)
        }.timeInMillis

    /**
     * Returns current date in the format h:mm a
     */
    fun getCurrentTime(): String = friendlyTimeFormat.format(Date())

    /**
     * Returns date millis in the format h:mm a
     */
    fun getDateMillisInFriendlyTimeFormat(dateMillis: Long): String = friendlyTimeFormat.format(Date(dateMillis))

    fun toIso8601Format(dateMillis: Long): String? =
        try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(Date(dateMillis))
        } catch (e: Exception) {
            "Error while parsing date in millis to Iso8601 string format".reportAsError(e)
            null
        }

    fun toDisplayMMMddYYYYDate(dateMillis: Long): String? =
        try {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(dateMillis))
        } catch (e: Exception) {
            "Date string argument is not a valid date".reportAsError(e)
            null
        }

    fun getDateFromIso8601String(isoStringDate: String): Date? =
        try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(isoStringDate)
        } catch (e: Exception) {
            "Date string argument is not a valid format".reportAsError(e)
            null
        }

    fun getDateUsingSiteTimeZone(isoStringDate: String): Date? {
        if (isoStringDate.isEmpty()) return null
        val iso8601DateString = iso8601OnSiteTimeZoneFromIso8601UTC(isoStringDate)
        return getDateFromFullDateString(iso8601DateString)
    }

    fun getCurrentDateInSiteTimeZone(): Date? {
        val site = selectedSite.getOrNull() ?: return null
        val targetTimezone = SiteUtils.getNormalizedTimezone(site.timezone).toZoneId()
        val currentDateTime = LocalDateTime.now()
        val zonedDateTime = ZonedDateTime.of(currentDateTime, ZoneId.systemDefault())
            .withZoneSameInstant(targetTimezone)
        // Format the result as a string
        val currentDateString = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return getDateFromFullDateString(currentDateString)
    }

    private fun getDateFromFullDateString(isoStringDate: String): Date? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
            formatter.parse(isoStringDate)
        } catch (e: Exception) {
            "Date string argument is not a valid format".reportAsError(e)
            null
        }
    }

    @Suppress("SwallowedException")
    private fun iso8601OnSiteTimeZoneFromIso8601UTC(iso8601date: String): String {
        return try {
            val site = selectedSite.getOrNull() ?: return iso8601date
            // Parse ISO 8601 string to LocalDateTime object
            val utcDateTime = LocalDateTime.parse(iso8601date, DateTimeFormatter.ISO_DATE_TIME)

            // Specify the target timezone
            val targetTimezone = SiteUtils.getNormalizedTimezone(site.timezone).toZoneId()

            val zonedDateTime = ZonedDateTime.of(utcDateTime, ZoneId.of("UTC"))
                .withZoneSameInstant(targetTimezone)

            // Format the result as a string
            zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            iso8601date
        }
    }

    /***
     * Will generate a formatted date string in two possible formats:
     * 1. If the date is today, it will return the time in the format h:mm a
     * 2. If the date is not today, it will return the date in the format MMM dd, yyyy
     */
    fun getDateOrTimeFromMillis(millis: Long): String? {
        val date = Date(millis)
        return if (isToday(date)) {
            getDateMillisInFriendlyTimeFormat(millis)
        } else {
            getDayMonthDateString(date.formatToYYYYmmDD())
        }
    }

    private fun isToday(date: Date): Boolean {
        val todayStart = getDateForTodayAtTheStartOfTheDay()
        return DateUtils.isSameDay(date, Date(todayStart))
    }

    companion object {
        const val ZERO = 0

        /**
         * Compares two dates to determine if [date2] is after [date1]. Note that
         * this method strips the time information from the comparison and is only comparing
         * the dates.
         *
         * @param date1 the base date for comparison
         * @param date2 the date to determine if after [date1]
         */
        fun isAfterDate(date1: Date, date2: Date): Boolean {
            val dateOnly1 = DateUtils.round(date1, Calendar.DATE)
            val dateOnly2 = DateUtils.round(date2, Calendar.DATE)
            return dateOnly2.after(dateOnly1)
        }
    }
}
