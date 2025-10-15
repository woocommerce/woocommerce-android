package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingResource
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val bookingsRepository: BookingsRepository,
    private val bookingMapper: BookingMapper,
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val booking = bookingsRepository.observeBooking(navArgs.bookingId)
        .shareIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val resource = booking.flatMapLatest { booking ->
        booking?.resourceId?.let { bookingsRepository.observeResource(it) } ?: flowOf(null)
    }

    private val loadingState = MutableStateFlow<BookingDetailsLoadingState>(BookingDetailsLoadingState.Idle)

    // Temporary, the booking status should come from the stored object
    private val bookingAttendanceStatus = MutableStateFlow<BookingAttendanceStatus?>(null)
    private val cancelState = MutableStateFlow<CancelState>(CancelState.Idle)
    private val showCancelDialog = MutableStateFlow(false)

    val state: LiveData<BookingDetailsViewState> = combine(
        booking,
        bookingAttendanceStatus,
        loadingState,
        resource,
        showCancelDialog,
        cancelState,
    ) { booking, attendanceStatus, loadingState, resource, showDialog, cancel ->
        with(bookingMapper) {
            val cancelMessage = booking?.let {
                buildCancelDialogMessage(booking, resourceProvider)
            } ?: ""
            BookingDetailsViewState(
                toolbarTitle = booking?.id?.value?.let { id ->
                    resourceProvider.getString(R.string.booking_details_title, id)
                } ?: "",
                bookingUiState = if (booking != null) {
                    buildBookingUiState(booking, attendanceStatus, resource, loadingState, cancel)
                } else {
                    null
                },
                onCancelBooking = ::onCancelBooking,
                onAttendanceStatusSelected = ::onAttendanceStatusSelected,
                showCancelBookingDialog = showDialog,
                cancelDialogMessage = cancelMessage,
                onDismissCancelDialog = ::onDismissCancelDialog,
                onConfirmCancelBooking = ::onConfirmCancelBooking,
                loadingState = loadingState,
                onRefresh = ::fetchBooking,
            )
        }
    }.asLiveData()

    init {
        fetchBooking(BookingDetailsLoadingState.Loading)
    }

    private fun fetchBooking(
        initialLoadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Refreshing
    ) {
        launch {
            if (!networkStatus.isConnected()) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
                return@launch
            }

            loadingState.value = initialLoadingState

            val bookingTask = async {
                bookingsRepository.fetchBooking(navArgs.bookingId)
            }
            val resourceTask = async {
                val booking = booking.first() ?: bookingTask.await().getOrNull()
                val resourceId = booking?.resourceId?.takeIf { it != 0L } ?: return@async Result.success(Unit)
                bookingsRepository.fetchResource(resourceId)
            }

            if (awaitAll(bookingTask, resourceTask).any { it.isFailure }) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
            }

            loadingState.value = BookingDetailsLoadingState.Idle
        }
    }

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
        resource: BookingResource?,
        loadingState: BookingDetailsLoadingState,
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
        bookingsAppointmentDetails = booking.toAppointmentDetailsModel(
            staffMemberStatus = buildStaffMemberStatus(
                resourceId = booking.resourceId,
                resource = resource,
                loadingState = loadingState
            ),
            cancelState = cancelState
        ),
        bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(),
        bookingPaymentDetails = booking.order.paymentInfo?.toPaymentDetailsModel(booking.currency)
    )

    private fun buildStaffMemberStatus(
        resourceId: Long,
        resource: BookingResource?,
        loadingState: BookingDetailsLoadingState
    ): BookingStaffMemberStatus? {
        return when {
            resourceId == 0L -> null
            resource != null -> BookingStaffMemberStatus.Loaded(resource.name)
            loadingState == BookingDetailsLoadingState.Loading ||
                loadingState == BookingDetailsLoadingState.Refreshing -> BookingStaffMemberStatus.Loading

            else -> BookingStaffMemberStatus.Unavailable
        }
    }
}
