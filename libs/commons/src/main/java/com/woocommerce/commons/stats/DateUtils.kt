package com.woocommerce.commons.stats

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private const val DATE_FORMAT_DEFAULT = "yyyy-MM-dd"

    fun getYearMonthDayStringFromDate(date: Date): String = formatDate(DATE_FORMAT_DEFAULT, date)

    /**
     * returns a [String] formatted
     * based on {@param pattern} and {@param date}
     */
    fun formatDate(
        pattern: String,
        date: Date
    ): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.ROOT)
        return dateFormat.format(date)
    }
}
