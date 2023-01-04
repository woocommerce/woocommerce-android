package com.woocommerce.android.extensions

import com.woocommerce.android.util.DateUtils
import java.util.Calendar
import java.util.Date

fun Calendar.startOfCurrentDay(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
    }.time

fun Calendar.endOfCurrentDay(): Date =
    (clone() as Calendar).apply { setToDayLastSecond() }.time

fun Calendar.startOfCurrentWeek(): Date =
    apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }.time

fun Calendar.endOfCurrentWeek(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek + DateUtils.DAYS_TAIL_IN_WEEK)
        setToDayLastSecond()
    }.time

fun Calendar.startOfCurrentMonth(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        setToDayFirstSecond()
    }.time

fun Calendar.endOfCurrentMonth(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        setToDayLastSecond()
    }.time

fun Calendar.startOfCurrentQuarter(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        setToDayFirstSecond()
        set(Calendar.MONTH, get(Calendar.MONTH) / DateUtils.THREE * DateUtils.THREE)
    }.time

fun Calendar.endOfCurrentQuarter(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.MONTH, get(Calendar.MONTH) / DateUtils.THREE * DateUtils.THREE + 2)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        setToDayLastSecond()
    }.time

fun Calendar.startOfCurrentYear(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_YEAR, DateUtils.ONE)
    }.time

fun Calendar.endOfCurrentYear(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
        setToDayLastSecond()
    }.time

private fun Calendar.setToDayFirstSecond() {
    set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
    set(Calendar.DAY_OF_MONTH, DateUtils.ONE)
}

private fun Calendar.setToDayLastSecond() {
    set(Calendar.SECOND, getMaximum(Calendar.SECOND))
    set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
    set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
}

private fun Calendar.clearMinutesAndSeconds() {
    clear(Calendar.MILLISECOND)
    clear(Calendar.SECOND)
    clear(Calendar.MINUTE)
}
