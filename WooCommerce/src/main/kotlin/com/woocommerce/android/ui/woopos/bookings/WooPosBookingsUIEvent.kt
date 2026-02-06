package com.woocommerce.android.ui.woopos.bookings

sealed interface WooPosBookingsUIEvent {
    data class BookingActionClicked(val action: WooPosBookingsState.BookingAction) : WooPosBookingsUIEvent
    data object PayByCardClicked : WooPosBookingsUIEvent
}
