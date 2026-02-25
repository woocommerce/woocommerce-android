package com.woocommerce.android.ui.woopos.bookings

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.Instant
import javax.inject.Inject

class WooPosBookingTimeRangeFormatter @Inject constructor() {
    fun format(start: Instant, end: Instant): String {
        val locale = ULocale.getDefault()
        val formatter = DateIntervalFormat.getInstance("jm", locale)
        /**
         * the WooCommerce Bookings API returns Unix timestamps where the value represents site-local time encoded as
         * UTC (fake UTC). For example, an 11:00 AM Vilnius booking (UTC+2) has a timestamp that decodes
         * to 11:00 UTC — not 09:00 UTC as true UTC would be.
         * https://github.com/woocommerce/woocommerce-android/pull/15423
         */
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val interval = DateInterval(start.toEpochMilli(), end.toEpochMilli())
        return formatter.format(interval).replace(" – ", "-")
    }
}
