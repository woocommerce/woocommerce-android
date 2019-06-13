package com.woocommerce.android.helpers

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WCDateTimeTestUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'"

    fun getCurrentDateTime(): Date = getCalendarInstance().time

    fun getCurrentDateTimeMinusDays(days: Int): Date {
        return getCalendarInstance().apply {
            add(Calendar.DATE, -days)
        }.time
    }

    fun getCurrentDateTimeMinusMonths(month: Int): Date {
        return getCalendarInstance().apply {
            add(Calendar.MONTH, -month)
        }.time
    }

    fun formatDate(date: Date): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
        return simpleDateFormat.format(date)
    }

    private fun getCalendarInstance(): Calendar {
        return Calendar.getInstance().apply {
            time = Date()
        }
    }
}
