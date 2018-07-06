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
    private val friendlyMonthDayFormat by lazy { SimpleDateFormat("MMM d", Locale.getDefault()) }
    private val dayOfWeekOfYearFormat by lazy { SimpleDateFormat("yyyy-'W'ww-u", Locale.getDefault()) }
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
     */
    fun getNumberOfDaysInMonth(iso8601Date: String): Int {
        try {
            val (year, month) = iso8601Date.split("-")
            // -1 because Calendar months are zero-based
            val calendar = GregorianCalendar(year.toInt(), month.toInt() - 1, 1)
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Date string is not of format YYYY-MM-DD: $iso8601Date")
        }
    }

    /**
     * Given an ISO8601 date of format YYYY-MM-DD, returns the String in "MMM d" format.
     *
     * For example, given 2018-07-03 returns "Jul 3", and given 2018-07-28 returns "Jul 28".
     */
    fun getFriendlyMonthDayString(iso8601Date: String): String {
        val (year, month, day) = iso8601Date.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        return friendlyMonthDayFormat.format(date)
    }

    /**
     * Given a date of format YYYY-'W'WW, returns the String in "MMM d" format, with the day being the first
     * day of that week (a Monday, by ISO8601 convention).
     *
     * For example, given 2018-W11, returns "Mar 12".
     */
    fun getFriendlyMonthDayStringForWeek(iso8601Week: String): String {
        val date = dayOfWeekOfYearFormat.parse("$iso8601Week-1")
        return friendlyMonthDayFormat.format(date)
    }

    /**
     * Given a date of format YYYY-MM, returns the corresponding short month format.
     *
     * For example, given 2018-07, returns "Jul".
     */
    fun getFriendlyMonthString(iso8601Month: String): String {
        val month = iso8601Month.split("-").last()
        return shortMonths[month.toInt() - 1]
    }
}
