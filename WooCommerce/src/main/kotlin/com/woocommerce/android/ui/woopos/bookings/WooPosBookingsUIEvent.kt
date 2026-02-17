package com.woocommerce.android.ui.woopos.bookings

sealed interface WooPosBookingsUIEvent {
    data class BookingMenuActionClicked(val action: WooPosBookingsState.BookingAction) : WooPosBookingsUIEvent
    data class AttendanceToggled(val attended: Boolean) : WooPosBookingsUIEvent
    data object CollectPaymentClicked : WooPosBookingsUIEvent
    data object AddBookingNoteClicked : WooPosBookingsUIEvent
    data class CopyEmailClicked(val email: String) : WooPosBookingsUIEvent
    data object CancelBookingConfirmed : WooPosBookingsUIEvent
    data object CancelBookingDismissed : WooPosBookingsUIEvent
}
