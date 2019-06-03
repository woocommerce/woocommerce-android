package com.woocommerce.android.helpers

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object WcDateTimeTestUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'"
    private val timeZone = TimeZone.getTimeZone("UTC")

    fun getCurrentDateTime(): Date = Calendar.getInstance(timeZone).time

    fun getCurrentDateTimeMinusDays(days: Int): Date {
        val calendar = Calendar.getInstance(timeZone)
        calendar.add(Calendar.DATE, -days)
        return calendar.time
    }

    fun getCurrentDateTimeMinusMonths(month: Int): Date {
        val calendar = Calendar.getInstance(timeZone)
        calendar.add(Calendar.MONTH, -month)
        return calendar.time
    }

    fun formatDate(date: Date): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT)
        simpleDateFormat.timeZone = timeZone
        return simpleDateFormat.format(date)
    }
}
