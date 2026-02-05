package com.woocommerce.android.ui.woopos.bookings

sealed interface WooPosBookingsUIEvent {
    data class BookingActionClicked(val action: WooPosBookingsState.OrderAction) : WooPosBookingsUIEvent
}
