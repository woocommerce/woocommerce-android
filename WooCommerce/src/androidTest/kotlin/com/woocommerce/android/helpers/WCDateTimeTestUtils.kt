package com.woocommerce.android.helpers

import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object WCDateTimeTestUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'"

    fun getCurrentDateTime(): Date = DateTimeUtils.nowUTC()

    fun getCurrentDateTimeMinusDays(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = DateTimeUtils.nowUTC()
        calendar.add(Calendar.DATE, -days)
        return calendar.time
    }

    fun getCurrentDateTimeMinusMonths(month: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = DateTimeUtils.nowUTC()
        calendar.add(Calendar.MONTH, -month)
        return calendar.time
    }

    fun formatDate(date: Date): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat.format(date)
    }
}
