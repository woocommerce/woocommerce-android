package com.woocommerce.android.extensions

import java.text.DateFormatSymbols
import java.util.GregorianCalendar

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
 * Method to convert month string from yyyy-MM format to yyyy
 * i.e. 2019-08 is formatted to 2019
 */
fun String.formatMonthToYear(): String {
    val (year, month, day) = this.split("-")
    val date = GregorianCalendar(year.toInt(), month.toInt() - 1, day.toInt()).time
    return date.formatToYYYY()
}

/**
 * Method to convert month string from yyyy-MM-dd format to d
 * i.e. 2019-08-08 is formatted to 08
 */
fun String.formatDateToDay(): String {
    val (_, _, date) = this.split("-")
    return date
}

/**
 * Method to convert month string from yyyy-MM-dd format to MMMM
 * i.e. 2019-08-08 is formatted to August
 */
fun String.formatMonthToFriendlyLongMonth(): String {
    val (_, month, _) = this.split("-")
    return DateFormatSymbols().months[month.toInt() - 1]
}
