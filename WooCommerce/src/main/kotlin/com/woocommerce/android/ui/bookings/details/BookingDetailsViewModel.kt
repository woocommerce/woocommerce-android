package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
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

    private val resourceLoadingState = MutableStateFlow<ResourceLoadingState>(ResourceLoadingState.Idle)
    private val resource = booking.flatMapLatest { it?.let { bookingsRepository.observeResource(it) } ?: flowOf(null) }

    private val loadingState = MutableStateFlow<BookingDetailsLoadingState>(BookingDetailsLoadingState.Idle)

    // Temporary, the booking status should come from the stored object
    private val bookingAttendanceStatus = MutableStateFlow<BookingAttendanceStatus?>(null)

    val state: LiveData<BookingDetailsViewState> = combine(
        booking,
        bookingAttendanceStatus,
        loadingState,
        resource,
        resourceLoadingState
    ) { booking, attendanceStatus, loadingState, resource, resourceLoadingState ->
        with(bookingMapper) {
            BookingDetailsViewState(
                toolbarTitle = booking?.id?.value?.let { id ->
                    resourceProvider.getString(R.string.booking_details_title, id)
                } ?: "",
                bookingUiState = if (booking != null) {
                    buildBookingUiState(booking, resource, attendanceStatus, resourceLoadingState)
                } else {
                    null
                },
                loadingState = loadingState,
                onCancelBooking = ::onCancelBooking,
                onAttendanceStatusSelected = ::onAttendanceStatusSelected,
                onRefresh = ::fetchBookingDetails,
            )
        }
    }.asLiveData()

    init {
        fetchBookingDetails(BookingDetailsLoadingState.Loading)
    }

    private fun fetchBookingDetails(
        initialBookingLoadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Refreshing
    ) {
        suspend fun fetchBooking(initialLoadingState: BookingDetailsLoadingState): Result<Unit> {
            loadingState.value = initialLoadingState
            return bookingsRepository.fetchBooking(navArgs.bookingId).also {
                loadingState.value = BookingDetailsLoadingState.Idle
            }
        }

        suspend fun fetchResource(): Result<Unit> {
            val resourceId = this.booking.first()?.resourceId?.takeIf { it != 0L } ?: return Result.success(Unit)
            resourceLoadingState.value = ResourceLoadingState.Loading
            return bookingsRepository.fetchResource(resourceId).also {
                resourceLoadingState.value = ResourceLoadingState.Idle
            }
        }

        launch {
            if (!networkStatus.isConnected()) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
                return@launch
            }

            val bookingTask = async { fetchBooking(initialBookingLoadingState) }
            val resourceTask = async { fetchResource() }
            if (awaitAll(bookingTask, resourceTask).any { it.isFailure }) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
            }
        }
    }

    private fun onAttendanceStatusSelected(status: BookingAttendanceStatus) {
        // Temporary, the booking status should come from the stored object
        bookingAttendanceStatus.value = status
    }

    private fun onCancelBooking() {
        // TODO Add logic to Cancel booking
    }

    private suspend fun BookingMapper.buildBookingUiState(
        booking: Booking,
        resource: BookingResource?,
        attendanceStatus: BookingAttendanceStatus?,
        resourceLoadingState: ResourceLoadingState
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
                loadingState = resourceLoadingState
            )
        ),
        bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(),
        bookingPaymentDetails = booking.order.paymentInfo?.toPaymentDetailsModel(booking.currency)
    )

    private fun buildStaffMemberStatus(
        resourceId: Long,
        resource: BookingResource?,
        loadingState: ResourceLoadingState
    ): BookingStaffMemberStatus? {
        return when {
            resourceId == 0L -> null
            resource != null -> BookingStaffMemberStatus.Loaded(resource.name)
            loadingState == ResourceLoadingState.Loading -> BookingStaffMemberStatus.Loading
            else -> BookingStaffMemberStatus.Unavailable
        }
    }

    private enum class ResourceLoadingState {
        Idle, Loading
    }
}
