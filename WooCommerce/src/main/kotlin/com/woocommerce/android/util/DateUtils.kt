package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import com.woocommerce.android.R
import com.woocommerce.android.model.TimeGroup
import org.wordpress.android.util.DateTimeUtils
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

object DateUtils {
    val friendlyMonthDayFormat by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }
    private val weekOfYearStartingMondayFormat by lazy {
        SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).apply {
            calendar = Calendar.getInstance().apply {
                // Ensure the date formatter follows ISO8601 week standards:
                // the first day of a week is a Monday, and the first week of the year starts on the first Monday
                // (and not on the Monday of the week containing January 1st, which may be in the previous year)
                firstDayOfWeek = Calendar.MONDAY
                minimalDaysInFirstWeek = 7
            }
        }
    }
    private val shortMonths by lazy { DateFormatSymbols().shortMonths }

    /**
     * Returns a string in the format of {date} at {time}.
     */
    fun getFriendlyShortDateAtTimeString(context: Context, rawDate: String): String {
        val date = DateTimeUtils.dateUTCFromIso8601(rawDate) ?: Date()
        val timeGroup = TimeGroup.getTimeGroupForDate(date)
        val dateLabel = when (timeGroup) {
            TimeGroup.GROUP_TODAY -> {
                context.getString(R.string.date_timeframe_today).toLowerCase()
            }
            TimeGroup.GROUP_YESTERDAY -> {
                context.getString(R.string.date_timeframe_yesterday).toLowerCase()
            }
            else -> {
                DateFormat.getDateFormat(context).format(date)
            }
        }
        val timeString = DateFormat.getTimeFormat(context).format(date.time)
        return context.getString(R.string.date_at_time, dateLabel, timeString)
    }

    fun getFriendlyLongDateAtTimeString(context: Context, rawDate: String): String {
        val date = DateTimeUtils.dateUTCFromIso8601(rawDate) ?: Date()
        val timeGroup = TimeGroup.getTimeGroupForDate(date)
        val dateLabel = when (timeGroup) {
            TimeGroup.GROUP_TODAY -> {
                context.getString(R.string.date_timeframe_today).toLowerCase()
            }
            TimeGroup.GROUP_YESTERDAY -> {
                context.getString(R.string.date_timeframe_yesterday).toLowerCase()
            }
            else -> {
                DateFormat.getLongDateFormat(context).format(date)
            }
        }
        val timeString = DateFormat.getTimeFormat(context).format(date.time)
        return context.getString(R.string.date_at_time, dateLabel, timeString)
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the number of days in the given month.
     *
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getNumberOfDaysInMonth(iso8601Date: String): Int {
        try {
            val (year, month) = iso8601Date.split("-")
            // -1 because Calendar months are zero-based
            val calendar = GregorianCalendar(year.toInt(), month.toInt() - 1, 1)
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-MM-DD: $iso8601Date")
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in short month ("MMM d") format.
     *
     * For example, given 2018-07-03 returns "Jul 3", and given 2018-07-28 returns "Jul 28".
     *
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getShortMonthDayString(iso8601Date: String): String {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            friendlyMonthDayFormat.format(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-MM-DD: $iso8601Date")
        }
    }

    /**
     * Given a date of format YYYY-'W'WW, returns the String in short month ("MMM d") format,
     * with the day being the first day of that week (a Monday, by ISO8601 convention).
     *
     * For example, given 2018-W11, returns "Mar 12".
     *
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getShortMonthDayStringForWeek(iso8601Week: String): String {
        return try {
            val date = weekOfYearStartingMondayFormat.parse(iso8601Week)
            friendlyMonthDayFormat.format(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-'W'WW: $iso8601Week")
        }
    }

    /**
     * Given a date of format YYYY-MM, returns the corresponding short month format.
     *
     * For example, given 2018-07, returns "Jul".
     *
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getShortMonthString(iso8601Month: String): String {
        val month = iso8601Month.split("-")[1]
        return try {
            shortMonths[month.toInt() - 1]
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-MM: $iso8601Month")
        }
    }

    /**
     * Given a date of format YYYY-MM, returns whether it's on a weekend
     *
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun isWeekend(iso8601Date: String): Boolean {
        return try {
            val (year, month, day) = iso8601Date.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt())
            val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
            (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-MM: $iso8601Date")
        }
    }

    /**
     * returns string of the current date
     * in the format: YYYY-MM-dd
     *
     */
    fun getCurrentDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        val dayOfMonth = calendar.get(Calendar.DATE)
        val year = calendar.get(Calendar.YEAR)
        val monthOfYear = calendar.get(Calendar.MONTH)
        return getFormattedDateString(year, monthOfYear, dayOfMonth)
    }

    /**
     * Given a year integer, month integer, date integer,
     * returns string in the format: YYYY-MM-dd
     *
     */
    fun getFormattedDateString(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format("%d-%02d-%02d", year, (month + 1), dayOfMonth)
    }

    /**
     * Given a date string of format YYYY-MM-dd, returns a [Calendar] instance of the same
     *
     */
    fun getCalendarInstance(value: String): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DATE, value.split("-")[2].toInt())
        cal.set(Calendar.MONTH, (value.split("-")[1].toInt()))
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.YEAR, value.split("-")[0].toInt())
        return cal
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in short month ("MMM d, YYYY") format.
     *
     * For example, given 2018-07-03 returns "Jul 3, 2018", and given 2018-07-28 returns "Jul 28, 2018".
     *
     */
    fun getShortDisplayDateString(dateString: String?): String? {
        return dateString?.let {
            val calendar = getCalendarInstance(dateString)
            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
            String.format("%s %s, %s",
                    calendar.get(Calendar.DATE),
                    month,
                    calendar.get(Calendar.YEAR))
        }
    }
}
