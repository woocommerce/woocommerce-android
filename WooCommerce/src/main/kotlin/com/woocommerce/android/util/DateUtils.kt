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

    private val yyyyMMddFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

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
     * Given a date of format YYYY-MM-DD, returns the string in a localized full long date format.
     *
     * @throws IllegalArgumentException if the argument is not a valid YYYY-MM-DD date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getLocalizedLongDateString(context: Context, dateString: String): String {
        return try {
            val (year, month, day) = dateString.split("-")
            val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
            DateFormat.getLongDateFormat(context).format(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format YYYY-MM-DD: $dateString")
        }
    }

    /**
     * Given a date string in localized format, returns a date object.
     *
     * @throws IllegalArgumentException is thronw by the [DateFormat] class if [dateString] cannot be
     * properly parsed
     */
    @Throws(IllegalArgumentException::class)
    fun getDateFromLocalizedLongDateString(context: Context, dateString: String): Date {
        val df = DateFormat.getLongDateFormat(context)
        return df.parse(dateString)
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
     * Given a date of format MMMM d, YYYY, returns date string in yyyy-MM-dd format
     *
     * @throws IllegalArgumentException if the argument is not a valid date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getDateString(dateString: String): String {
        return try {
            val originalFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.ROOT)
            val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val date = originalFormat.parse(dateString)
            targetFormat.format(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format MMMM dd, yyyy: $dateString")
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
     * @throws IllegalArgumentException if the argument is not a valid iso8601 date string.
     */
    @Throws(IllegalArgumentException::class)
    fun getShortHourString(iso8601Date: String): String {
        return try {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd HH", Locale.ROOT)
            val targetFormat = SimpleDateFormat("hha", Locale.ROOT)
            val date = originalFormat.parse(iso8601Date)
            targetFormat.format(date).toLowerCase().trimStart('0')
        } catch (e: Exception) {
            throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd H: $iso8601Date")
        }
    }
}
