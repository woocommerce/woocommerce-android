package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val bookingsRepository: BookingsRepository,
    private val bookingMapper: BookingMapper,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val booking = bookingsRepository.observeBooking(navArgs.bookingId)

    private val loadingState = MutableStateFlow<BookingDetailsLoadingState>(BookingDetailsLoadingState.Idle)

    // Temporary, the booking status should come from the stored object
    private val bookingAttendanceStatus = MutableStateFlow<BookingAttendanceStatus?>(null)

    val state: LiveData<BookingDetailsViewState> = combine(
        booking,
        bookingAttendanceStatus,
        loadingState
    ) { booking, attendanceStatus, loadingState ->
        with(bookingMapper) {
            BookingDetailsViewState(
                toolbarTitle = booking?.id?.value?.let { id ->
                    resourceProvider.getString(R.string.booking_details_title, id)
                } ?: "",
                bookingUiState = if (booking != null) buildBookingUiState(booking, attendanceStatus) else null,
                loadingState = loadingState,
                onCancelBooking = ::onCancelBooking,
                onAttendanceStatusSelected = ::onAttendanceStatusSelected
            )
        }
    }.asLiveData()

    init {
        refreshBooking()
    }

    private fun refreshBooking() {
        launch {
            loadingState.value = BookingDetailsLoadingState.Refreshing
            bookingsRepository.fetchBooking(navArgs.bookingId)
            loadingState.value = BookingDetailsLoadingState.Idle
        }
    }

    private fun onAttendanceStatusSelected(status: BookingAttendanceStatus) {
        // Temporary, the booking status should come from the stored object
        bookingAttendanceStatus.value = status
    }

    private fun onCancelBooking() {
        // TODO Add logic to Cancel booking
    }

    private fun BookingMapper.buildBookingUiState(
        booking: Booking,
        attendanceStatus: BookingAttendanceStatus?
    ): BookingUiState = BookingUiState(
        orderId = booking.orderId,
        bookingSummary = booking.toBookingSummaryModel().let {
            if (attendanceStatus != null) {
                it.copy(attendanceStatus = attendanceStatus)
            } else {
                it
            }
        },
        bookingsAppointmentDetails = booking.toAppointmentDetailsModel(),
        bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(),
        bookingPaymentDetails = BookingPaymentDetailsModel(
            service = "$55.00",
            tax = "$4.50",
            discount = "-",
            total = "$59.50"
        )
    )
}
