package com.woocommerce.android.ui.woopos.bookings

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import android.icu.util.ULocale
import com.woocommerce.android.extensions.clock
import com.woocommerce.android.tools.SelectedSite
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class WooPosBookingTimeRangeFormatter @Inject constructor(
    private val selectedSite: SelectedSite,
) {
    private val storeZoneId: ZoneId by lazy { selectedSite.get().clock.zone }

    fun format(start: Instant, end: Instant): String {
        val locale = ULocale.getDefault()
        val formatter = DateIntervalFormat.getInstance("jm", locale)
        formatter.timeZone = TimeZone.getTimeZone(storeZoneId.id)
        val interval = DateInterval(start.toEpochMilli(), end.toEpochMilli())
        return formatter.format(interval).replace(" – ", "-")
    }
}
