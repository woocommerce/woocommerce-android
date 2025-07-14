package com.woocommerce.android.wear.extensions

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Date.formatToMMMddYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d, yyyy",
    locale
).format(this)

fun Date.formatToYYYYmmDDhhmmss(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(this)

fun Date.oneDayAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneDayAgo
        add(Calendar.DATE, -1)
    }.time
