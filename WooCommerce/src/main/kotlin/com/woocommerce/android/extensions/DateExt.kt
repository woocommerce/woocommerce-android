package com.woocommerce.android.extensions

import com.woocommerce.android.util.DateUtils
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.formatToYYYYmm(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(this)

fun Date.formatToYYYYmmDD(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(this)

fun Date.formatToYYYY(): String = SimpleDateFormat("yyyy", Locale.getDefault()).format(this)

fun Date.formatToYYYYWmm(): String = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(this)

fun Date.formatToMMMMdd(): String = SimpleDateFormat("MMMM dd", Locale.getDefault()).format(this)

fun Date.formatToDD(): String = SimpleDateFormat("d", Locale.getDefault()).format(this)

fun Date.formatToMMMdd(): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(this)

fun Date.formatToMMMddYYYY(): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(this)

fun Date.formatToMMMddYYYYhhmm(): String = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault()).format(this)

fun Date.formatToEEEEMMMddhha(): String {
    val symbols = DateFormatSymbols(Locale.getDefault())
    symbols.amPmStrings = arrayOf("am", "pm")
    val dateFormat = SimpleDateFormat("EEEE, MMM dd › ha", Locale.getDefault())
    dateFormat.dateFormatSymbols = symbols
    return dateFormat.format(this)
}

fun Date?.offsetGmtDate(gmtOffset: Float) = this?.let { DateUtils.offsetGmtDate(it, gmtOffset) }

fun Date.formatToYYYYmmDDhhmmss(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(this)
