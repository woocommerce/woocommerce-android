package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    resourceProvider: ResourceProvider,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val _state = MutableStateFlow(
        BookingDetailsViewState(
            onMarkAsPaid = ::onMarkAsPaid,
            onMarkAsRefunded = ::onMarkAsRefunded,
            onCancelBooking = ::onCancelBooking,
        )
    )
    val state: LiveData<BookingDetailsViewState> = _state.asLiveData()

    init {
        _state.update {
            it.copy(
                toolbarTitle = resourceProvider.getString(R.string.booking_details_title, navArgs.bookingId),
            )
        }
    }

    fun onAttendanceStatusSelected(status: BookingAttendanceStatus) {
        _state.update { current ->
            current.copy(
                bookingSummary = current.bookingSummary.copy(attendanceStatus = status)
            )
        }
    }

    private fun onMarkAsPaid() {
        _state.update { current ->
            current.copy(
                bookingSummary = current.bookingSummary.copy(status = BookingStatus.Paid)
            )
        }
    }

    private fun onMarkAsRefunded() {
        _state.update { current ->
            current.copy(
                bookingSummary = current.bookingSummary.copy(status = BookingStatus.Unpaid)
            )
        }
    }

    private fun onCancelBooking() {
        // TODO Add logic to Cancel booking
    }
}
