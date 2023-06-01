package com.woocommerce.android.util

import java.util.TimeZone

class TimezoneProvider {
    val deviceTimezone: TimeZone
        get() = TimeZone.getDefault()
}
