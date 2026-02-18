package com.woocommerce.android.ui.woopos.bookings

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.ULocale
import java.time.Instant
import javax.inject.Inject

class WooPosBookingTimeRangeFormatter @Inject constructor() {
    fun format(start: Instant, end: Instant): String {
        val locale = ULocale.getDefault()
        val formatter = DateIntervalFormat.getInstance("jm", locale)
        val interval = DateInterval(start.toEpochMilli(), end.toEpochMilli())
        return formatter.format(interval).replace(" – ", "-")
    }
}
