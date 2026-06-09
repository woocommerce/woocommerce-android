package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.extensions.clock
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Error.BookingAttendanceChangeError
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Error.BookingCancelError
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Error.BookingNoteAddError
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingAddNoteTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingAttendanceChanged
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingCancelled
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDateCalendarSelected
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDateNextTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingDatePreviousTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingListItemTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingNoteAdded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BookingViewOrderTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.reflect.KClass

class WooPosBookingsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val clock: Clock,
    selectedSite: SelectedSite,
) {
    private val storeZoneId = selectedSite.get().clock.zone

    suspend fun trackListItemTapped() {
        analyticsTracker.track(BookingListItemTapped)
    }

    suspend fun trackBookingCancelled() {
        analyticsTracker.track(BookingCancelled)
    }

    suspend fun trackBookingCancelFailed(errorContext: KClass<out Any>, error: Throwable) {
        analyticsTracker.track(
            BookingCancelError(
                errorContext = errorContext,
                errorType = null,
                errorDescription = error.message,
            )
        )
    }

    suspend fun trackAttendanceChanged() {
        analyticsTracker.track(BookingAttendanceChanged)
    }

    suspend fun trackAttendanceChangeFailed(errorContext: KClass<out Any>, error: Throwable) {
        analyticsTracker.track(
            BookingAttendanceChangeError(
                errorContext = errorContext,
                errorType = null,
                errorDescription = error.message,
            )
        )
    }

    suspend fun trackAddNoteTapped() {
        analyticsTracker.track(BookingAddNoteTapped)
    }

    suspend fun trackNoteAdded() {
        analyticsTracker.track(BookingNoteAdded)
    }

    suspend fun trackNoteAddFailed(errorContext: KClass<out Any>, error: Throwable) {
        analyticsTracker.track(
            BookingNoteAddError(
                errorContext = errorContext,
                errorType = null,
                errorDescription = error.message,
            )
        )
    }

    suspend fun trackDatePreviousTapped(selectedDate: LocalDate) {
        analyticsTracker.track(BookingDatePreviousTapped(deltaFromToday(selectedDate)))
    }

    suspend fun trackDateNextTapped(selectedDate: LocalDate) {
        analyticsTracker.track(BookingDateNextTapped(deltaFromToday(selectedDate)))
    }

    suspend fun trackDateCalendarSelected(selectedDate: LocalDate) {
        analyticsTracker.track(BookingDateCalendarSelected(deltaFromToday(selectedDate)))
    }

    suspend fun trackViewOrderTapped() {
        analyticsTracker.track(BookingViewOrderTapped)
    }

    private fun deltaFromToday(date: LocalDate): Long {
        val today = Instant.now(clock).atZone(storeZoneId).toLocalDate()
        return ChronoUnit.DAYS.between(today, date)
    }
}
