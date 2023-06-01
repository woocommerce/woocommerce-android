package com.woocommerce.android.util

import java.util.TimeZone
import javax.inject.Inject

class TimezoneProvider @Inject constructor() {
    val deviceTimezone: TimeZone
        get() = TimeZone.getDefault()
}
