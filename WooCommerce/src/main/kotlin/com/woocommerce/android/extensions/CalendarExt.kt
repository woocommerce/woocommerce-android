package com.woocommerce.android.extensions

import com.woocommerce.android.util.DateUtils
import java.util.Calendar
import java.util.Date

fun Calendar.startOfCurrentDay(): Date =
    (clone() as Calendar)
        .apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        }.time

fun Calendar.endOfCurrentDay(): Date =
    (clone() as Calendar)
        .apply {
            set(Calendar.SECOND, getMaximum(Calendar.SECOND))
            set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
            set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
        }.time

fun Calendar.startOfCurrentWeek(): Date =
    (clone() as Calendar)
        .apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.time

fun Calendar.endOfCurrentWeek(): Date =
    (clone() as Calendar)
        .apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek + DateUtils.DAYS_TAIL_IN_WEEK)
            set(Calendar.SECOND, getMaximum(Calendar.SECOND))
            set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
            set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
        }.time

fun Calendar.startOfCurrentMonth(): Date =
    (clone() as Calendar)
        .apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
            set(Calendar.DAY_OF_MONTH, DateUtils.ONE)
        }.time

fun Calendar.endOfCurrentMonth(): Date =
    (clone() as Calendar)
        .apply {
            clear(Calendar.MILLISECOND)
            clear(Calendar.SECOND)
            clear(Calendar.MINUTE)
            set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.SECOND, getMaximum(Calendar.SECOND))
            set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
            set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
        }.time
