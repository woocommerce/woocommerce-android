package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingResource
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.isAttendanceStatusEditable
import javax.inject.Inject
import com.woocommerce.android.extensions.combine as woocombine

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val bookingsRepository: BookingsRepository,
    private val bookingMapper: BookingMapper,
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {

    private var bookingFetchJob: Job? = null

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val bookingId: Long? = (navArgs.mode as? BookingDetailsFragment.Mode.ShowBooking)?.bookingId
    private val booking = if (bookingId != null) {
        bookingsRepository.observeBooking(bookingId)
            .distinctUntilChanged()
            .onEach {
                if (it == null) {
                    fetchBooking(bookingId, BookingDetailsLoadingState.Loading)
                }
            }
            .shareIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)
    } else {
        flowOf(null)
    }

    private val resource = booking.flatMapLatest { booking ->
        booking?.resourceId?.let { bookingsRepository.observeResource(it) } ?: flowOf(null)
    }

    private val loadingState = MutableStateFlow<BookingDetailsLoadingState>(BookingDetailsLoadingState.Idle)

    private val attendanceUpdateStatus = MutableStateFlow<AttendanceUpdateStatus>(AttendanceUpdateStatus.Idle)

    private val cancelStatusState = MutableStateFlow<CancelStatus>(CancelStatus.Idle)
    private val showCancelBookingDialog = MutableStateFlow(false)

    private val paymentUpdateStatus = MutableStateFlow<PaymentUpdateStatus>(PaymentUpdateStatus.Idle)

    private val cancelBookingDialogState = combine(
        booking,
        showCancelBookingDialog,
    ) { booking, showCancelBooking ->
        if (showCancelBooking && booking != null) {
            val message = bookingMapper.buildCancelDialogMessage(booking)
            DialogState(
                title = UiString.UiStringRes(R.string.booking_cancel_dialog_title),
                message = message,
                positiveButton = DialogState.DialogButton(
                    text = UiString.UiStringRes(R.string.booking_cancel_dialog_confirm),
                    onClick = { onConfirmCancelBooking(booking.id.value) }
                ),
                negativeButton = DialogState.DialogButton(
                    text = UiString.UiStringRes(R.string.booking_cancel_dialog_keep),
                    onClick = ::onDismissCancelDialog
                ),
            )
        } else {
            null
        }
    }

    private val bookingUiStateFlow = woocombine(
        booking,
        attendanceUpdateStatus,
        loadingState,
        resource,
        cancelStatusState,
        paymentUpdateStatus,
    ) { booking, attendanceUpdate, loadingState, resource, cancelStatus, paymentUpdate ->
        if (booking != null) {
            bookingMapper.buildBookingUiState(
                booking = booking,
                attendanceUpdateStatus = attendanceUpdate,
                resource = resource,
                loadingState = loadingState,
                cancelStatus = cancelStatus,
                paymentUpdateStatus = paymentUpdate,
            )
        } else {
            null
        }
    }

    val state: LiveData<BookingDetailsViewState> = combine(
        booking,
        bookingUiStateFlow,
        loadingState,
        cancelBookingDialogState,
        paymentUpdateStatus,
    ) { booking, bookingUiState, loadingState, cancelBookingDialog, paymentUpdateStatus ->
        when (val mode = navArgs.mode) {
            BookingDetailsFragment.Mode.Empty -> BookingDetailsViewState.Empty
            is BookingDetailsFragment.Mode.ShowBooking -> {
                with(bookingMapper) {
                    BookingDetailsViewState.ShowBooking(
                        toolbarTitle = booking?.id?.value?.let { id ->
                            resourceProvider.getString(R.string.booking_details_title, id)
                        } ?: "",
                        bookingUiState = bookingUiState,
                        dialogState = cancelBookingDialog,
                        loadingState = loadingState,
                        onRefresh = { fetchBooking(mode.bookingId) },
                    )
                }
            }
        }
    }.asLiveData()

    init {
        when (val mode = navArgs.mode) {
            is BookingDetailsFragment.Mode.ShowBooking -> {
                fetchBooking(mode.bookingId, BookingDetailsLoadingState.Loading)
            }

            else -> Unit
        }
    }

    private fun fetchBooking(
        bookingId: Long,
        initialLoadingState: BookingDetailsLoadingState = BookingDetailsLoadingState.Refreshing
    ) {
        bookingFetchJob?.cancel()
        bookingFetchJob = launch {
            if (navArgs.mode !is BookingDetailsFragment.Mode.ShowBooking) {
                return@launch
            }
            if (!networkStatus.isConnected()) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
                return@launch
            }
            loadingState.value = initialLoadingState

            coroutineScope {
                val bookingTask = async {
                    bookingsRepository.fetchBooking(bookingId)
                }
                val resourceTask = async {
                    val booking = booking.first() ?: bookingTask.await().getOrNull()
                    val resourceId = booking?.resourceId?.takeIf { it != 0L } ?: return@async Result.success(Unit)
                    bookingsRepository.fetchResource(resourceId)
                }

                if (awaitAll(bookingTask, resourceTask).any { it.isFailure }) {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
                }
            }
            loadingState.value = BookingDetailsLoadingState.Idle
        }
    }

    private fun onAttendanceStatusSelected(
        bookingId: Long,
        status: BookingAttendanceStatus
    ) {
        launch {
            if (!networkStatus.isConnected()) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
                return@launch
            }
            attendanceUpdateStatus.value = AttendanceUpdateStatus.InProgress
            val attendanceStatus = status.toDataModel() ?: return@launch
            bookingsRepository.updateAttendanceStatus(
                bookingId = bookingId,
                attendanceStatus = attendanceStatus
            ).onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_attendance_status_error))
            }
            attendanceUpdateStatus.value = AttendanceUpdateStatus.Idle
        }
    }

    private fun BookingAttendanceStatus.toDataModel(): BookingEntity.AttendanceStatus? = when (this) {
        BookingAttendanceStatus.Booked -> BookingEntity.AttendanceStatus.Booked
        BookingAttendanceStatus.CheckedIn -> BookingEntity.AttendanceStatus.CheckedIn
        BookingAttendanceStatus.NoShow -> BookingEntity.AttendanceStatus.NoShow
        BookingAttendanceStatus.Cancelled -> null
    }

    private fun onCancelBooking() {
        showCancelBookingDialog.value = true
    }

    private fun onDismissCancelDialog() {
        showCancelBookingDialog.value = false
    }

    private fun onConfirmCancelBooking(bookingId: Long) = launch {
        showCancelBookingDialog.value = false
        cancelStatusState.value = CancelStatus.InProgress
        bookingsRepository.cancelBooking(bookingId)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_cancel_error))
            }
        cancelStatusState.value = CancelStatus.Idle
    }

    private fun onMarkAsPaid(bookingId: Long) = launch {
        if (!networkStatus.isConnected()) {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            return@launch
        }
        paymentUpdateStatus.value = PaymentUpdateStatus.InProgress
        bookingsRepository.markAsPaid(bookingId)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_mark_as_paid_error))
            }
        paymentUpdateStatus.value = PaymentUpdateStatus.Idle
    }

    @Suppress("LongParameterList")
    private suspend fun BookingMapper.buildBookingUiState(
        booking: Booking,
        attendanceUpdateStatus: AttendanceUpdateStatus,
        resource: BookingResource?,
        loadingState: BookingDetailsLoadingState,
        cancelStatus: CancelStatus,
        paymentUpdateStatus: PaymentUpdateStatus,
    ): BookingUiState = BookingUiState(
        orderId = booking.orderId,
        bookingSummary = booking.toBookingSummaryModel(attendanceUpdateStatus),
        bookingsAppointmentDetails = booking.toAppointmentDetailsModel(
            staffMemberStatus = buildStaffMemberStatus(
                resourceId = booking.resourceId,
                resource = resource,
                loadingState = loadingState
            ),
            cancelStatus = cancelStatus
        ),
        bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(),
        bookingPaymentDetails = booking.order.paymentInfo?.toPaymentDetailsModel(booking.currency),
        note = booking.note,
        isAttendanceStatusEditable = booking.isAttendanceStatusEditable,
        onCancelBooking = ::onCancelBooking,
        onAttendanceStatusSelected = { attendanceStatus ->
            if (attendanceStatus.toDataModel() != booking.attendanceStatus) {
                onAttendanceStatusSelected(booking.id.value, attendanceStatus)
            }
        },
        onMarkAsPaid = { onMarkAsPaid(booking.id.value) },
        paymentUpdateStatus = paymentUpdateStatus,
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
