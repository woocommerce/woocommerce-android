package com.woocommerce.android.ui.woopos.bookings

sealed interface WooPosBookingsUIEvent {
    data class BookingActionClicked(val action: WooPosBookingsState.BookingAction) : WooPosBookingsUIEvent
    data class AttendanceToggled(val attended: Boolean) : WooPosBookingsUIEvent
    data object CollectPaymentClicked : WooPosBookingsUIEvent
    data object AddBookingNoteClicked : WooPosBookingsUIEvent
    data class CopyEmailClicked(val email: String) : WooPosBookingsUIEvent
}
