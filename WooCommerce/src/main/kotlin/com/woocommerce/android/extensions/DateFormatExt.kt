package com.woocommerce.android.extensions

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Method to convert date string from yyyy-MM-dd format to yyyy-MM format
 */
fun String.formatDateToYearMonth(): String {
    val (year, month, day) = this.split("-")
    val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
    return date.formatToYYYYmm()
}

/**
 * Method to convert date string from yyyy-MM-dd format to yyyy format
 */
fun String.formatDateToYear(): String {
    val (year, month, day) = this.split("-")
    val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
    return date.formatToYYYY()
}

/**
 * Method to convert date string from yyyy'W'MM'W'dd format to yyyy-'W'MM
 * i.e. 2019W08W08 is formatted to 2019-W32
 */
fun String.formatDateToWeeksInYear(): String {
    val (year, month, day) = this.split("W")
    val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
    return date.formatToYYYYWmm()
}

/**
 * Method to convert month string from yyyy-MM-dd HH format to EEEE, MMM dd›hha
 * i.e. 2018-08-08 07 is formatted to Wednesday, Aug 08›7am
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyDayHour(): String {
    return try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd HH", Locale.getDefault())
        val date = originalFormat.parse(this)
        date.formatToEEEEMMMddhha()
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd HH: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd format to MMMM
 * i.e. 2019-08-08 is formatted to 2019›August
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyLongMonthYear(): String {
    return try {
        val (year, month, _) = this.split("-")
        "$year › ${DateFormatSymbols().months[month.toInt() - 1]}"
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd format to MMMM dd
 * i.e. 2019-08-08 is formatted to August 08
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyLongMonthDate(): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToMMMMdd()
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}
