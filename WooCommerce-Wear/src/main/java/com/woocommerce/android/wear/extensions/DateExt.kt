package com.woocommerce.android.wear.extensions

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Abbreviated month, day and year in the locale's own order. */
fun Date.formatToLocalizedMonthDayYear(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(locale, "yMMMd"),
    locale
).format(this)

fun Date.formatToYYYYmmDDhhmmss(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(this)

fun Date.oneDayAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneDayAgo
        add(Calendar.DATE, -1)
    }.time
