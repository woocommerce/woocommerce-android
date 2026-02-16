package com.woocommerce.android.ui.woopos.bookings

sealed interface WooPosBookingsUIEvent {
    data class BookingActionClicked(val action: WooPosBookingsState.BookingAction) : WooPosBookingsUIEvent
    data class AttendanceToggled(val attended: Boolean) : WooPosBookingsUIEvent
    data object PayByCardClicked : WooPosBookingsUIEvent
    data object PayByCashClicked : WooPosBookingsUIEvent
    data object AddBookingNoteClicked : WooPosBookingsUIEvent
    data class CopyEmailClicked(val email: String) : WooPosBookingsUIEvent
    data class CopyPhoneClicked(val phone: String) : WooPosBookingsUIEvent
}
