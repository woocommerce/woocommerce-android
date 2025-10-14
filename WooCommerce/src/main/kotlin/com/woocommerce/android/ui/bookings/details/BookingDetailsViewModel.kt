package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    bookingsRepository: BookingsRepository,
    private val bookingMapper: BookingMapper,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val booking = bookingsRepository.observeBooking(navArgs.bookingId)

    // Temporary, the booking status should come from the stored object
    private val bookingAttendanceStatus = MutableStateFlow<BookingAttendanceStatus?>(null)
    private val cancelState = MutableStateFlow<CancelState>(CancelState.Idle)
    private val showCancelDialog = MutableStateFlow(false)

    val state: LiveData<BookingDetailsViewState> = combine(
        booking,
        bookingAttendanceStatus,
        showCancelDialog,
        cancelState,
    ) { booking, attendanceStatus, showDialog, cancel ->
        with(bookingMapper) {
            val cancelMessage = booking?.let {
                buildCancelDialogMessage(booking, resourceProvider)
            } ?: ""
            BookingDetailsViewState(
                toolbarTitle = booking?.id?.value?.let { id ->
                    resourceProvider.getString(R.string.booking_details_title, id)
                } ?: "",
                bookingUiState = if (booking != null) {
                    buildBookingUiState(booking, attendanceStatus, cancel)
                } else {
                    null
                },
                onCancelBooking = ::onCancelBooking,
                onAttendanceStatusSelected = ::onAttendanceStatusSelected,
                showCancelBookingDialog = showDialog,
                cancelDialogMessage = cancelMessage,
                onDismissCancelDialog = ::onDismissCancelDialog,
                onConfirmCancelBooking = ::onConfirmCancelBooking,
            )
        }
    }.asLiveData()

    private fun onAttendanceStatusSelected(status: BookingAttendanceStatus) {
        // Temporary, the booking status should come from the stored object
        bookingAttendanceStatus.value = status
    }

    private fun onCancelBooking() {
        showCancelDialog.value = true
    }

    private fun onDismissCancelDialog() {
        showCancelDialog.value = false
    }

    private fun onConfirmCancelBooking() = launch {
        // TODO Add logic to Cancel booking action
        showCancelDialog.value = false
        cancelState.value = CancelState.InProgress
        delay(Duration.ofSeconds(1).toMillis())
        cancelState.value = CancelState.Idle
    }

    private suspend fun BookingMapper.buildBookingUiState(
        booking: Booking,
        attendanceStatus: BookingAttendanceStatus?,
        cancelState: CancelState,
    ): BookingUiState = BookingUiState(
        orderId = booking.orderId,
        bookingSummary = booking.toBookingSummaryModel().let {
            if (attendanceStatus != null) {
                it.copy(attendanceStatus = attendanceStatus)
            } else {
                it
            }
        },
        bookingsAppointmentDetails = booking.toAppointmentDetailsModel(cancelState),
        bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(),
        bookingPaymentDetails = booking.order.paymentInfo?.toPaymentDetailsModel(booking.currency)
    )
}
