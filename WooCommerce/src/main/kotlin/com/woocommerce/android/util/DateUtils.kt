package com.woocommerce.android.util

import android.content.Context
import android.text.format.DateFormat
import com.woocommerce.android.R
import com.woocommerce.android.model.TimeGroup
import org.wordpress.android.util.DateTimeUtils
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

object DateUtils {
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
}
