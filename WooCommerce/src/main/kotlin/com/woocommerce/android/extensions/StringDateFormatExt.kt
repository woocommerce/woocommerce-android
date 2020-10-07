package com.woocommerce.android.extensions

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

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
 * Method to convert month string from yyyy-MM-dd HH format to EEEE, MMM dd›hha
 * i.e. 2018-08-08 07 is formatted to Wednesday, Aug 08›7am
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyDayHour(locale: Locale = Locale.getDefault()): String {
    return try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd HH", locale)
        val date = originalFormat.parse(this)
        date.formatToEEEEMMMddhha(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd HH: $this")
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
 * Method to convert month string from yyyy-MM-dd format to MMMM dd
 * i.e. 2019-08-08 is formatted to August 08
 */
@Throws(IllegalArgumentException::class)
fun String.formatDateToFriendlyLongMonthDate(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToMMMMdd(locale)
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
 * Method to convert month string from yyyy-MM-dd format to MMM d
 * i.e. 2019-08-08 is formatted to Aug 8
 */
@Throws(IllegalArgumentException::class)
fun String.formatToMonthDateOnly(locale: Locale = Locale.getDefault()): String {
    return try {
        val (year, month, day) = this.split("-")
        val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
        date.formatToMMMdd(locale)
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd'T'hh:mm:ss format to MMM dd
 * i.e. 2018-08-08T08:12:03 is formatted to Aug 08
 */
@Throws(IllegalArgumentException::class)
fun String?.formatDateToISO8601Format(locale: Locale = Locale.getDefault()): Date? {
    return try {
        if (!this.isNullOrEmpty()) {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", locale)
            return originalFormat.parse(this)
        }
        null
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd'T'HH:mm:ss: $this")
    }
}

/**
 * Method to convert month string from yyyy-MM-dd format to MMM dd
 * i.e. 2018-08-08T08:12:03 is formatted to Aug 08
 */
@Throws(IllegalArgumentException::class)
fun String?.formatDateToYYYYMMDDFormat(locale: Locale = Locale.getDefault()): Date? {
    return try {
        if (!this.isNullOrEmpty()) {
            val originalFormat = SimpleDateFormat("yyyy-MM-dd", locale)
            return originalFormat.parse(this)
        }
        null
    } catch (e: Exception) {
        throw IllegalArgumentException("Date string argument is not of format yyyy-MM-dd: $this")
    }
}
