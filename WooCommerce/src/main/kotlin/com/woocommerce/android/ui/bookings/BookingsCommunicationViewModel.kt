package com.woocommerce.android.ui.bookings

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

/**
 * Activity-scoped ViewModel used to communicate between Booking list and details in split-pane mode.
 */
class BookingsCommunicationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun pushEvent(event: CommunicationEvent) {
        triggerEvent(event)
    }

    sealed class CommunicationEvent : MultiLiveEvent.Event() {
        data class BookingSelected(val bookingId: Long) : CommunicationEvent()
    }
}
