package com.woocommerce.android.extensions

import java.util.Calendar
import java.util.Date

fun Calendar.startOfCurrentDay(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
    }.time

fun Calendar.endOfCurrentDay(): Date =
    (clone() as Calendar).apply { setToDayLastSecond() }.time

fun Calendar.startOfCurrentWeek(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }.time

fun Calendar.endOfCurrentWeek(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek + DAYS_TAIL_IN_WEEK)
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
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        setToDayLastSecond()
    }.time

fun Calendar.startOfCurrentQuarter(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        setToDayFirstSecond()
        set(Calendar.MONTH, get(Calendar.MONTH) / THREE * THREE)
    }.time

fun Calendar.endOfCurrentQuarter(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.MONTH, get(Calendar.MONTH) / THREE * THREE + 2)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        setToDayLastSecond()
    }.time

fun Calendar.startOfCurrentYear(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.DAY_OF_YEAR, 1)
    }.time

fun Calendar.endOfCurrentYear(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
        setToDayLastSecond()
    }.time

private fun Calendar.setToDayFirstSecond() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.DAY_OF_MONTH, 1)
}

private fun Calendar.setToDayLastSecond() {
    clear(Calendar.MILLISECOND)
    set(Calendar.SECOND, getMaximum(Calendar.SECOND))
    set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
    set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
}

private fun Calendar.clearMinutesAndSeconds() {
    clear(Calendar.MILLISECOND)
    clear(Calendar.SECOND)
    clear(Calendar.MINUTE)
}

private const val DAYS_TAIL_IN_WEEK = 6
private const val THREE = 3
