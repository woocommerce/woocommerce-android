package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingListItemTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import javax.inject.Inject

class WooPosBookingsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker
) {
    suspend fun trackListItemTapped() {
        analyticsTracker.track(BookingListItemTapped)
    }
}
