package com.woocommerce.android.extensions

import com.woocommerce.android.util.DateUtils
import java.util.*

fun Calendar.startOfCurrentDay(): Date =
    apply {
        clear(Calendar.MILLISECOND)
        clear(Calendar.SECOND)
        clear(Calendar.MINUTE)
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
    }.time

fun Calendar.endOfCurrentDay(): Date =
    apply {
        set(Calendar.SECOND, getMaximum(Calendar.SECOND))
        set(Calendar.MINUTE, getMaximum(Calendar.MINUTE))
        set(Calendar.HOUR_OF_DAY, getMaximum(Calendar.HOUR_OF_DAY))
    }.time

fun Calendar.startOfCurrentWeek(): Date =
    apply {
        clear(Calendar.MILLISECOND)
        clear(Calendar.SECOND)
        clear(Calendar.MINUTE)
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }.time

fun Calendar.endOfCurrentWeek(): Date =
    apply {
        clear(Calendar.MILLISECOND)
        clear(Calendar.SECOND)
        clear(Calendar.MINUTE)
        set(Calendar.HOUR_OF_DAY, DateUtils.ZERO)
        add(Calendar.WEEK_OF_YEAR, -1)
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek + DateUtils.DAYS_TAIL_IN_WEEK)
    }.time
