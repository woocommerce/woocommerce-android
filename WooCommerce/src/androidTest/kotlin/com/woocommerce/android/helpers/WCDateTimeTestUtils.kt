package com.woocommerce.android.helpers

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object WCDateTimeTestUtils {
    private const val DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'"

    fun getCurrentDateTime(): Date = Calendar.getInstance().time

    fun getCurrentDateTimeMinusDays(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -days)
        return calendar.time
    }

    fun getCurrentDateTimeMinusMonths(month: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -month)
        return calendar.time
    }

    fun formatDate(date: Date): String = SimpleDateFormat(DATE_FORMAT).format(date)
}
