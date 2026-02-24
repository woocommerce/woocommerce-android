package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingAttendanceChanged
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingCancelFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingCancelled
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDateCalendarSelected
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDateNextTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDatePreviousTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingIssueRefundTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingListItemTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingNoteAdded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import javax.inject.Inject

class WooPosBookingsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker
) {
    suspend fun trackListItemTapped() {
        analyticsTracker.track(BookingListItemTapped)
    }

    suspend fun trackBookingCancelled() {
        analyticsTracker.track(BookingCancelled)
    }

    suspend fun trackBookingCancelFailed() {
        analyticsTracker.track(BookingCancelFailed)
    }

    suspend fun trackAttendanceChanged() {
        analyticsTracker.track(BookingAttendanceChanged)
    }

    suspend fun trackNoteAdded() {
        analyticsTracker.track(BookingNoteAdded)
    }

    suspend fun trackDatePreviousTapped(deltaFromToday: Long) {
        analyticsTracker.track(BookingDatePreviousTapped(deltaFromToday))
    }

    suspend fun trackDateNextTapped(deltaFromToday: Long) {
        analyticsTracker.track(BookingDateNextTapped(deltaFromToday))
    }

    suspend fun trackDateCalendarSelected(deltaFromToday: Long) {
        analyticsTracker.track(BookingDateCalendarSelected(deltaFromToday))
    }

    suspend fun trackIssueRefundTapped() {
        analyticsTracker.track(BookingIssueRefundTapped)
    }
}
