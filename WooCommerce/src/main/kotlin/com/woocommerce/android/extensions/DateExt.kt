package com.woocommerce.android.extensions

import android.content.Context
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

fun Date.formatToYYYYmm(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "yyyy-MM",
    locale
).format(this)

/**
 * Formats the date to a full month and year string, e.g. "August 2026" in English and "2026年8月" in Japanese.
 */
fun Date.formatToLocalizedMonthYear(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(locale, "yMMMM"),
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

/**
 * Formats the date to a full month and day string, e.g. "August 13" in English and "13 August" in British English.
 */
fun Date.formatToLocalizedFullMonthDay(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(locale, "MMMMd"),
    locale
).format(this)

fun Date.formatToDD(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    "d",
    locale
).format(this)

/**
 * Formats the date to an abbreviated month and day string, e.g. "Aug 13" in English and "13 Ağu" in Turkish.
 */
fun Date.formatToLocalizedMonthDay(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(locale, "MMMd"),
    locale
).format(this)

/**
 * Formats the date to an abbreviated month, day and year string, e.g. "Aug 3, 2026" in English and
 * "3. Aug. 2026" in German. Pairs with [formatToLocalizedMonthDay], which keeps the month name in every locale.
 */
fun Date.formatToLocalizedMonthDayYear(locale: Locale = Locale.getDefault()): String = SimpleDateFormat(
    DateFormat.getBestDateTimePattern(locale, "yMMMd"),
    locale
).format(this)

/**
 * Formats the date with the locale's medium date style, e.g. "Aug 3, 2026" in English, "3 Aug 2026" in British
 * English and "03.08.2026" in German.
 */
fun Date.formatToLocalizedMedium(locale: Locale = Locale.getDefault()): String = SimpleDateFormat
    .getDateInstance(SimpleDateFormat.MEDIUM, locale)
    .format(this)

/**
 * Formats the date and time with the locale's medium date style and short time style, e.g. "Aug 3, 2026, 9:30 AM"
 * in English and "3 Aug 2026, 09:30" in British English. The time style follows the locale, not the system
 * 12h/24h setting.
 */
fun Date.formatToLocalizedMediumWithTime(locale: Locale = Locale.getDefault()): String = SimpleDateFormat
    .getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT, locale)
    .format(this)

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

/**
 * Same as [formatToYYYYmmDDhhmmss] but using the UTC timezone, for GMT API fields.
 */
fun Date.formatToYYYYmmDDhhmmssGmt(locale: Locale = Locale.ROOT): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(this)

val Date.pastTimeDeltaFromNowInDays
    get() = Calendar.getInstance().time
        .let { it.time - this.time }
        .takeIf { it >= 0 }
        ?.let { TimeUnit.DAYS.convert(it, MILLISECONDS) }
        ?.toInt()

fun Date.daysLater(daysLater: Int): Date = Calendar.getInstance()
    .apply { time = this@daysLater }
    .apply { add(Calendar.DATE, daysLater) }
    .time

fun Date.daysAgo(daysAgo: Int) = daysLater(-daysAgo)

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

/**
 * Formats the two dates as a range in the locale's own interval format, e.g. "Jul 1 – 31, 2022" in English
 * and "1.–31. Juli 2022" in German.
 */
fun Date.formatAsRangeWith(other: Date, locale: Locale): String = DateIntervalFormat
    .getInstance("yMMMd", locale)
    .format(DateInterval(this.time, other.time))

private const val THREE_MONTHS = 3
private const val SEVEN_DAYS = 7

fun LocalDate.formatStyleFull(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
