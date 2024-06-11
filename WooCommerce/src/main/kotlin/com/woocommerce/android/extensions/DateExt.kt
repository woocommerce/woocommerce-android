package com.woocommerce.android.extensions

import android.content.Context
import android.text.format.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

fun Date.formatToYYYYmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-MM",
    locale
).format(this)

fun Date.formatToMMMMyyyy(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMMM yyyy",
    locale
).format(this)

fun Date.formatToYYYYmmDD(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-MM-dd",
    locale
).format(this)

fun Date.formatToYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy",
    locale
).format(this)

fun Date.formatToYYYYWmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-'W'ww",
    locale
).format(this)

fun Date.formatToMMMMdd(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMMM dd",
    locale
).format(this)

fun Date.formatToDD(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "d",
    locale
).format(this)

fun Date.formatToMMMdd(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d",
    locale
).format(this)

fun Date.formatToDDMMMYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "dd MMM yyyy",
    locale
).format(this)

fun Date.formatToMMMddYYYY(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d, yyyy",
    locale
).format(this)

fun Date.formatToMMMddYYYYhhmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "MMM d, yyyy hh:mm a",
    locale
).format(this)

fun Date.formatToDDyyyy(locale: Locale): String = SimpleDateFormat(
    "d, yyyy",
    locale
).format(this)

fun Date.formatToEEEEMMMddhha(locale: Locale): String {
    val symbols = DateFormatSymbols(locale)
    symbols.amPmStrings = arrayOf("am", "pm")
    val dateFormat = SimpleDateFormat("EEEE, MMM dd › ha", locale)
    dateFormat.dateFormatSymbols = symbols
    return dateFormat.format(this)
}

fun Date.getTimeString(context: Context): String = DateFormat.getTimeFormat(context).format(this.time)

fun Date.getMediumDate(context: Context): String = DateFormat.getMediumDateFormat(context).format(this)

/**
 * Formats the date to a string in the format "yyyy-MM-dd'T'HH:mm:ss".
 *
 * @param locale The locale to use for formatting the date, defaults to [Locale.ROOT], as this is mostly used for API
 * requests.
 */
fun Date.formatToYYYYmmDDhhmmss(locale: Locale = Locale.ROOT): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale).format(this)

val Date.pastTimeDeltaFromNowInDays
    get() = Calendar.getInstance().time
        .let { it.time - this.time }
        .takeIf { it >= 0 }
        ?.let { TimeUnit.DAYS.convert(it, MILLISECONDS) }
        ?.toInt()

fun Date.daysAgo(daysAgo: Int) =
    Calendar.getInstance()
        .apply { time = this@daysAgo }
        .apply { add(Calendar.DATE, -daysAgo) }
        .time

fun Date.oneDayAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneDayAgo
        add(Calendar.DATE, -1)
    }.time

fun Date.oneWeekAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneWeekAgo
        add(Calendar.DATE, -SEVEN_DAYS)
    }.time

fun Date.oneMonthAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneMonthAgo
        add(Calendar.MONTH, -1)
    }.time

fun Date.oneQuarterAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneQuarterAgo
        add(Calendar.MONTH, -THREE_MONTHS)
    }.time

fun Date.oneYearAgo(): Date =
    Calendar.getInstance().apply {
        time = this@oneYearAgo
        add(Calendar.YEAR, -1)
    }.time

fun Date.isInSameYearAs(other: Date, baseCalendar: Calendar): Boolean {
    val calendar = baseCalendar.clone() as Calendar
    calendar.time = this
    val thisYear = calendar.get(Calendar.YEAR)
    calendar.time = other
    val otherYear = calendar.get(Calendar.YEAR)
    return thisYear == otherYear
}

fun Date.isInSameMonthAs(other: Date, baseCalendar: Calendar): Boolean {
    val calendar = baseCalendar.clone() as Calendar
    calendar.time = this
    val thisMonth = calendar.get(Calendar.MONTH)
    calendar.time = other
    val otherMonth = calendar.get(Calendar.MONTH)
    return thisMonth == otherMonth && isInSameYearAs(other, calendar)
}

fun Date.formatAsRangeWith(other: Date, locale: Locale, calendar: Calendar): String {
    val formattedStartDate = if (this.isInSameYearAs(other, calendar)) {
        this.formatToMMMdd(locale)
    } else {
        this.formatToMMMddYYYY(locale)
    }

    val formattedEndDate = if (this.isInSameMonthAs(other, calendar)) {
        other.formatToDDyyyy(locale)
    } else {
        other.formatToMMMddYYYY(locale)
    }

    return "$formattedStartDate – $formattedEndDate"
}

private const val THREE_MONTHS = 3
private const val SEVEN_DAYS = 7

fun LocalDate.formatStyleFull(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
