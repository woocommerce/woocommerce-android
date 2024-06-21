package com.woocommerce.android.wear.extensions

import java.util.Calendar
import java.util.Date

fun Calendar.startOfCurrentDay(): Date =
    (clone() as Calendar).apply {
        clearMinutesAndSeconds()
        set(Calendar.HOUR_OF_DAY, 0)
    }.time

fun Calendar.endOfCurrentDay(): Date =
    (clone() as Calendar).apply { setToDayLastSecond() }.time

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
