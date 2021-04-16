package com.woocommerce.android.extensions

import android.content.Context
import android.text.format.DateFormat
import com.woocommerce.android.util.CrashUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.apache.commons.lang3.time.DateUtils.isSameDay
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

fun Date.formatToYYYYmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-MM", locale
).format(this)

fun Date.formatToYYYYmmDD(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-MM-dd", locale
).format(this)

fun Date.formatToYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy", locale
).format(this)

fun Date.formatToYYYYWmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-'W'ww", locale
).format(this)

fun Date.formatToMMMMdd(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMMM dd", locale
).format(this)

fun Date.formatToDD(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "d", locale
).format(this)

fun Date.formatToMMMdd(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d", locale
).format(this)

fun Date.formatToMMMddYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d, yyyy", locale
).format(this)

fun Date.formatToMMMddYYYYhhmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d, yyyy hh:mm a", locale
).format(this)

fun Date.formatToEEEEMMMddhha(locale: Locale): String {
    val symbols = DateFormatSymbols(locale)
    symbols.amPmStrings = arrayOf("am", "pm")
    val dateFormat = SimpleDateFormat("EEEE, MMM dd › ha", locale)
    dateFormat.dateFormatSymbols = symbols
    return dateFormat.format(this)
}

fun Date.isToday() =
    try {
        isSameDay(Date(), this)
    } catch (e: Exception) {
        with("Unable to match dateString with today date. (current dateString value: $this)") {
            WooLog.e(T.UTILS, this)
            CrashUtils.logException(e, T.UTILS, this)
        }
        null
    }

fun Date.getTimeString(context: Context): String = DateFormat.getTimeFormat(context).format(this.time)

fun Date.getMediumDate(context: Context): String = DateFormat.getMediumDateFormat(context).format(this)

fun Date?.offsetGmtDate(gmtOffset: Float) = this?.let { DateUtils.offsetGmtDate(it, gmtOffset) }

fun Date.formatToYYYYmmDDhhmmss(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(this)

val Date.pastTimeDeltaFromNowInDays
    get() = Calendar.getInstance().time
        .let { it.time - this.time }
        .takeIf { it >= 0 }
        ?.let { TimeUnit.DAYS.convert(it, MILLISECONDS) }
        ?.toInt()
