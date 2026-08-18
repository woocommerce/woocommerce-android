package com.woocommerce.android.extensions

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * Method to convert date string from yyyy-MM-dd format to yyyy-MM format
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToYearMonth(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToYYYYmm(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert date string from yyyy-MM-dd format to yyyy format
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToYear(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToYYYY(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert date string from yyyy'W'MM'W'dd format to yyyy-'W'MM
 * i.e. 2019W08W08 is formatted to 2019-W32
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToWeeksInYear(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("W")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToYYYYWmm(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy'W'MM'W'dd: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd format to MMMM
 * i.e. 2019-08-08 is formatted to 2019›August
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyLongMonthYear(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, _) = this.split("-")
        "$year › ${DateFormatSymbols(locale).months[month.toInt() - 1]}"
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd format to dd
 * i.e. 2019-08-08 is formatted to 8
 */
@Throws(IllegalArgumentException::class)
fun String.formatToDateOnly(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToDD(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd'T'hh:mm:ss format to Date object
 */
@Throws(IllegalArgumentException::class)
fun String?.parseGmtDateFromIso8601DateFormat(locale: Locale = Locale.getDefault()): Date? {
    return try {
        if (!this.isNullOrEmpty()) {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return originalFormat.parse(this)
        }
        null
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd'T'HH:mm:ss: $this")
    }
}
