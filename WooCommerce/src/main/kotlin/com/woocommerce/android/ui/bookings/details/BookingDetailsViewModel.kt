package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.automattic.eventhorizon.BookingDetailAddNoteTapEvent
import com.automattic.eventhorizon.BookingDetailAttendanceStatusUpdateEvent
import com.automattic.eventhorizon.BookingDetailCancelBookingEvent
import com.automattic.eventhorizon.BookingDetailRefundTapEvent
import com.automattic.eventhorizon.BookingDetailViewLinkedOrderTapEvent
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingAnalyticsHelper
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.BookingResource
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingLocationStatus
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.toEventHorizonValue
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
import org.wordpress.android.fluxc.persistence.entity.isReschedulable
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val bookingsRepository: BookingsRepository,
    private val bookingMapper: BookingMapper,
    private val networkStatus: NetworkStatus,
    private val paymentStatusResolver: PaymentStatusResolver,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val orderDetailRepository: OrderDetailRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ScopedViewModel(savedState) {

    private val analyticsHelper = BookingAnalyticsHelper()

    private var bookingFetchJob: Job? = null
    private var attendanceUpdateJob: Job? = null

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

    private val cancelBookingDialogState = combine(
        booking,
        showCancelBookingDialog,
    ) { booking, showCancelBooking ->
        if (showCancelBooking && booking != null) {
            val message = bookingMapper.buildCancelDialogMessage(booking)
            DialogState(
                title = UiString.UiStringRes(R.string.booking_cancel_dialog_title_v2),
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

    private val bookingUiStateFlow = combine(
        booking,
        attendanceUpdateStatus,
        loadingState,
        resource,
        cancelStatusState
    ) { booking, attendanceUpdate, loadingState, resource, cancelStatus ->
        if (booking != null) {
            bookingMapper.buildBookingUiState(
                booking = booking,
                attendanceUpdateStatus = attendanceUpdate,
                resource = resource,
                loadingState = loadingState,
                cancelStatus = cancelStatus,
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
    ) { booking, bookingUiState, loadingState, cancelBookingDialog ->
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
                val locationTask = async {
                    val booking = booking.first() ?: bookingTask.await().getOrNull() ?: return@async
                    val productId = booking.productId.takeIf { it != 0L } ?: return@async
                    bookingsRepository.fetchProductBookingLocation(
                        productId = productId,
                        bookingId = booking.id.value
                    )
                }

                if (awaitAll(bookingTask, resourceTask).any { it.isFailure }) {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
                }
                locationTask.await()
            }
            loadingState.value = BookingDetailsLoadingState.Idle
        }
    }

    private fun onAttendanceStatusSelected(
        bookingId: Long,
        status: BookingAttendanceStatus
    ) {
        attendanceUpdateJob?.cancel()
        attendanceUpdateJob = appScope.launch {
            if (!networkStatus.isConnected()) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
                return@launch
            }
            analyticsTrackerWrapper.track(
                BookingDetailAttendanceStatusUpdateEvent(
                    bookingStatus = status.toEventHorizonValue()
                )
            )
            attendanceUpdateStatus.value = AttendanceUpdateStatus.InProgress
            val attendanceStatus = status.toDataModel()
            bookingsRepository.updateAttendanceStatus(
                bookingId = bookingId,
                attendanceStatus = attendanceStatus
            ).onFailure {
                with(analyticsHelper) {
                    analyticsTrackerWrapper.trackError(
                        event = AnalyticsEvent.BOOKING_LIST_FAILED_TO_UPDATE_BOOKING_DETAILS,
                        throwable = it,
                        errorContext = this@BookingDetailsViewModel::class.java.simpleName,
                        additionalProperties = mapOf(BookingAnalyticsHelper.KEY_ACTION to "update_attendance")
                    )
                }
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_attendance_status_error))
            }
            attendanceUpdateStatus.value = AttendanceUpdateStatus.Idle
        }
    }

    private fun BookingAttendanceStatus.toDataModel(): BookingEntity.AttendanceStatus = when (this) {
        BookingAttendanceStatus.Attended -> BookingEntity.AttendanceStatus.Attended
        BookingAttendanceStatus.Unattended -> BookingEntity.AttendanceStatus.Unattended
    }

    private fun onRescheduleBooking() {
        bookingId?.let { triggerEvent(NavigateToRescheduleBooking(it)) }
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
        analyticsTrackerWrapper.track(BookingDetailCancelBookingEvent)
        bookingsRepository.cancelBooking(bookingId)
            .onFailure {
                with(analyticsHelper) {
                    analyticsTrackerWrapper.trackError(
                        event = AnalyticsEvent.BOOKING_LIST_FAILED_TO_UPDATE_BOOKING_DETAILS,
                        throwable = it,
                        errorContext = this@BookingDetailsViewModel::class.java.simpleName,
                        additionalProperties = mapOf(BookingAnalyticsHelper.KEY_ACTION to "cancel_booking")
                    )
                }
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_cancel_error))
            }
        cancelStatusState.value = CancelStatus.Idle
    }

    private fun openBookingNote(bookingId: Long) {
        analyticsTrackerWrapper.track(BookingDetailAddNoteTapEvent)
        triggerEvent(NavigateToBookingNote(bookingId))
    }

    private fun openOrderDetails(orderId: Long) {
        analyticsTrackerWrapper.track(BookingDetailViewLinkedOrderTapEvent)
        triggerEvent(NavigateToOrder(orderId))
    }

    private fun issueRefund(orderId: Long) {
        analyticsTrackerWrapper.track(BookingDetailRefundTapEvent)
        triggerEvent(NavigateToIssueRefund(orderId))
    }

    fun onRefundCompleted() {
        bookingId?.let { fetchBooking(it) }
        launch {
            val orderId = booking.first()?.orderId?.takeIf { it != 0L } ?: return@launch
            orderDetailRepository.fetchOrderById(orderId)
            orderDetailRepository.fetchOrderRefunds(orderId)
        }
    }

    private suspend fun BookingMapper.buildBookingUiState(
        booking: Booking,
        attendanceUpdateStatus: AttendanceUpdateStatus,
        resource: BookingResource?,
        loadingState: BookingDetailsLoadingState,
        cancelStatus: CancelStatus,
    ): BookingUiState {
        val bookingId = booking.id.value
        val orderId = booking.orderId
        val paymentStatus = paymentStatusResolver.resolve(orderId)
        return BookingUiState(
            orderId = orderId,
            bookingSummary = booking.toBookingSummaryModel(paymentStatus, attendanceUpdateStatus),
            bookingsAppointmentDetails = booking.toAppointmentDetailsModel(
                staffMemberStatus = buildStaffMemberStatus(
                    resourceId = booking.resourceId,
                    resource = resource,
                    loadingState = loadingState
                ),
                cancelStatus = cancelStatus,
                rescheduleButtonVisible = featureFlagRepository.isEnabled(FeatureFlag.BOOKINGS_RESCHEDULE) &&
                    booking.isReschedulable,
                attendanceUpdateStatus = attendanceUpdateStatus,
                locationStatus = buildLocationStatus(booking, loadingState),
            ),
            bookingCustomerDetails = booking.order.customerInfo.toCustomerDetailsModel(booking.customerNote),
            bookingPaymentDetails = booking.order.paymentInfo?.toPaymentDetailsModel(booking.currency),
            note = booking.note,
            isAttendanceStatusEditable = booking.isAttendanceStatusEditable,
            onCancelBooking = ::onCancelBooking,
            onRescheduleBooking = ::onRescheduleBooking,
            onAttendanceToggle = {
                val targetStatus = when (booking.attendanceStatus) {
                    BookingEntity.AttendanceStatus.Attended -> BookingAttendanceStatus.Unattended
                    else -> BookingAttendanceStatus.Attended
                }
                onAttendanceStatusSelected(bookingId, targetStatus)
            },
            onViewOrderClicked = { openOrderDetails(orderId) },
            onNoteClicked = { openBookingNote(bookingId) },
            onIssueRefundClicked = if (
                (paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.PARTIALLY_REFUNDED) &&
                orderId != 0L
            ) {
                { issueRefund(orderId) }
            } else {
                null
            },
        )
    }

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

    private fun buildLocationStatus(
        booking: Booking,
        loadingState: BookingDetailsLoadingState
    ): BookingLocationStatus {
        return when {
            booking.location != null -> BookingLocationStatus.Loaded(requireNotNull(booking.location))
            loadingState == BookingDetailsLoadingState.Loading ||
                loadingState == BookingDetailsLoadingState.Refreshing -> BookingLocationStatus.Loading

            else -> BookingLocationStatus.Unavailable
        }
    }

    data class NavigateToOrder(val orderId: Long) : MultiLiveEvent.Event()
    data class NavigateToIssueRefund(val orderId: Long) : MultiLiveEvent.Event()
    data class NavigateToBookingNote(val bookingId: Long) : MultiLiveEvent.Event()
    data class NavigateToRescheduleBooking(val bookingId: Long) : MultiLiveEvent.Event()
}
