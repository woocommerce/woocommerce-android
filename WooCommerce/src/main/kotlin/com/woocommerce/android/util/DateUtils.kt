package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.util.WooLog.T.UTILS
import org.apache.commons.lang3.time.DateUtils
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import javax.inject.Inject

class DateUtils @Inject constructor(
    private val locale: Locale,
    private val crashLogger: CrashLogging
) {
    private val friendlyMonthDayFormat: SimpleDateFormat = SimpleDateFormat("MMM d", locale)
    private val friendlyMonthDayYearFormat: SimpleDateFormat = SimpleDateFormat("MMM d, yyyy", locale)

    private val weekOfYearStartingMondayFormat: SimpleDateFormat = SimpleDateFormat("yyyy-'W'ww", locale).apply {
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
     * Given a date string in localized format, returns a date object.
     *
     * return null from the [DateFormat] class if [dateString] cannot be
     * properly parsed
     */
    fun getDateFromLocalizedLongDateString(context: Context, dateString: String): Date? {
        val df = DateFormat.getLongDateFormat(context)
        return df.parse(dateString)
    }

    /**
     * Given a date of format YYYY-'W'WW, returns the String in short month ("MMM d") format,
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
            "Date string argument is not of format YYYY-'W'WW: $iso8601Week".reportAsError(e)
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
        val month = iso8601Month.split("-")[1]
        return try {
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
            targetFormat.format(date!!).toLowerCase(locale).trimStart('0')
        } catch (e: Exception) {
            "Date string argument is not of format yyyy-MM-dd H: $iso8601Date".reportAsError(e)
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
     * Converts the given [year] [month] [day] to a [Date] object
     * and applies the passed [gmtOffset] to the date
     *
     * [timeAtStartOfDay] is set to true for start date and set to false for end dates
     */
    fun localDateToGmt(
        year: Int,
        month: Int,
        day: Int,
        gmtOffset: Float,
        timeAtStartOfDay: Boolean
    ): Date {
        val hour = if (timeAtStartOfDay) 0 else 23
        val minuteSecond = if (timeAtStartOfDay) 0 else 59
        val operator: (Int, Int) -> Int = if (timeAtStartOfDay) Int::minus else Int::plus
        val date = GregorianCalendar(year, month, day, hour, minuteSecond, minuteSecond).time
        return offsetGmtDate(date, gmtOffset, operator)
    }

    /**
     * Converts the given [dateString] to a [Date] object
     * and applies the passed [gmtOffset] to the date
     */
    fun localDateToGmt(
        dateString: String,
        gmtOffset: Float,
        timeAtStartOfDay: Boolean
    ): Date? {
        return try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", locale)
            val date = dateFormat.parse(dateString) ?: Date()

            val calendar = Calendar.getInstance()
            calendar.time = date

            localDateToGmt(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH),
                day = calendar.get(Calendar.DATE),
                gmtOffset = gmtOffset,
                timeAtStartOfDay = timeAtStartOfDay
            )
        } catch (e: Exception) {
            "Date string argument is not of format MMM dd, yyyy: $dateString".reportAsError(e)
            return null
        }
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
        crashLogger.sendReport(exception = exception)
    }

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

    /**
     * Returns a date object with the date for the first day of the current week
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfWeek(calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.time

    /**
     * Returns a date object with the date for the first day of the current month
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfMonth(calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.DAY_OF_MONTH, ONE)
        }.time

    /**
     * Returns a date object with the date for the first day of the current quarter
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfQuarter(calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.DAY_OF_MONTH, ONE)
            set(Calendar.MONTH, get(Calendar.MONTH) / THREE * THREE)
        }.time

    /**
     * Returns a date object with the date for the first day of the current year
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfYear(calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.DAY_OF_MONTH, ONE)
            set(Calendar.MONTH, ZERO)
        }.time

    /**
     * Returns a date object with the date for the first day of the previous N minusWeeks argument or 1 week
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfPreviousWeek(minusWeeks: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.WEEK_OF_YEAR, -minusWeeks)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.time

    /**
     * Returns a date object with the date for the first day of the previous N minusMonths argument or 1 month
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfPreviousMonth(minusMonths: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.MONTH, -minusMonths)
            set(Calendar.DAY_OF_MONTH, ONE)
        }.time

    /**
     * Returns a date object with the date for the first day of the previous N minusQuarter argument or 1 quarter
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfPreviousQuarter(minusQuarter: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, get(Calendar.MONTH) / THREE * THREE)
            add(Calendar.MONTH, -minusQuarter * THREE)
        }.time

    /**
     * Returns a date object with the date for the first day of N minusQuarter argument years or the previous year
     * of calendar argument or current calendar
     */
    fun getDateForFirstDayOfPreviousYear(minusYears: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.YEAR, -minusYears)
            set(Calendar.DAY_OF_MONTH, ONE)
            set(Calendar.MONTH, ZERO)
        }.time

    /**
     * Returns a date object with the date for the last day of the previous N minusWeeks argument or 1 week
     *  of calendar argument or current calendar
     */
    fun getDateForLastDayOfPreviousWeek(minusWeeks: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.WEEK_OF_YEAR, -minusWeeks)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek + DAYS_TAIL_IN_WEEK)
        }.time

    /**
     * Returns a date object with the date for the last day of the previous N minusMonths argument or 1 month
     *  of calendar argument or current calendar
     */
    fun getDateForLastDayOfPreviousMonth(minusMonths: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.MONTH, -minusMonths)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.time

    /**
     * Returns a date object with the date for the last day of the previous quarter
     * of calendar argument or current calendar
     */
    fun getDateForLastDayOfPreviousQuarter(minusQuarter: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.DAY_OF_YEAR, -DAYS_IN_QUARTER * minusQuarter)
            set(Calendar.DAY_OF_MONTH, ONE)
            set(Calendar.MONTH, get(Calendar.MONTH) / THREE * THREE + 2)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.time

    fun getDateForLastDayOfPreviousYear(minusYears: Int = 1, calendar: Calendar = Calendar.getInstance()): Date =
        calendar.apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            set(Calendar.MONTH, getActualMaximum(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            add(Calendar.YEAR, -minusYears)
        }.time

    fun getCurrentDateTimeMinusDays(days: Int): Long =
        Calendar.getInstance().apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(Calendar.DATE, -days)
        }.timeInMillis

    /**
     * Returns current date
     */
    fun getCurrentDate() = Date()

    /**
     * Returns a Calendar object with argument date applied argument operation
     */
    fun getDateTimeAppliedOperation(date: Date, operationOver: Int, number: Int): Date =
        Calendar.getInstance().apply {
            time = date
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, ZERO)
            add(operationOver, number)
        }.time

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

    companion object {
        const val DAYS_IN_QUARTER = 90
        const val DAYS_TAIL_IN_WEEK = 6

        const val ZERO = 0
        const val ONE = 1
        const val THREE = 3

        /**
         * Returns a date with the passed GMT offset applied - note that this assumes the passed date is GMT
         *
         * The [operator] can either be [Int::plus] or [Int::minus]
         * [Int::plus] is passed when formatting gmtDate to local date
         * [Int::minus] is passed when formatting local date to gmt
         */
        fun offsetGmtDate(dateGmt: Date, gmtOffset: Float, operator: (Int, Int) -> Int = Int::plus): Date {
            if (gmtOffset == 0f) {
                return dateGmt
            }

            val secondsOffset = (3600 * gmtOffset).toInt() // 3600 is the number of seconds in an hour
            val calendar = Calendar.getInstance()
            calendar.time = dateGmt
            calendar.set(Calendar.SECOND, operator(calendar.get(Calendar.SECOND), secondsOffset))
            return calendar.time
        }

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

        fun getDayOfWeekWithMonthAndDayFromDate(date: Date): String {
            val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.US)
            return dateFormat.format(date)
        }
    }
}
