package com.woocommerce.android.extensions

import java.util.TimeZone
import java.util.concurrent.TimeUnit

val TimeZone.offsetInHours: Int
    get() = rawOffset.toLong().let {
        TimeUnit.HOURS.convert(it, TimeUnit.MILLISECONDS)
    }.toInt()
