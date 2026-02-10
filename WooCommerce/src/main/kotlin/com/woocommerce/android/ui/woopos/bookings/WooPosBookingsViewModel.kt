package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.bookings.list.BookingListHandler
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val bookingListHandler: BookingListHandler,
    private val dateTimeProvider: DateTimeProvider,
) : ViewModel() {

    companion object {
        private const val MIN_LOADING_DURATION_MS = 300L
    }

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var selectedBookingId: Long? = null
    private var fetchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        observeBookings()
        fetchBookings()
    }

    private fun fetchBookings() {
        fetchJob?.cancel()
        loadMoreJob?.cancel()
        fetchJob = viewModelScope.launch {
            val result = bookingListHandler.loadBookings(
                sortBy = BookingListSortOption.NewestToOldest
            )
            result.onFailure {
                if (_state.value is WooPosBookingsState.Loading) {
                    _state.value = WooPosBookingsState.Error(
                        message = it.message ?: "Failed to load bookings"
                    )
                }
            }.onSuccess {
                if (_state.value is WooPosBookingsState.Loading) {
                    _state.value = WooPosBookingsState.Empty()
                }
            }
        }
    }

    private fun observeBookings() {
        viewModelScope.launch {
            bookingListHandler.bookingsFlow.collectLatest { bookings ->
                if (bookings.isEmpty() && _state.value is WooPosBookingsState.Loading) {
                    return@collectLatest
                }

                if (bookings.isEmpty()) {
                    _state.value = WooPosBookingsState.Empty()
                    return@collectLatest
                }

                if (selectedBookingId == null) {
                    selectedBookingId = bookings.first().id.value
                }

                val items = bookings.associate { booking ->
                    mapToItemViewState(booking) to mapToDetailsViewState(booking)
                }

                val selectedDetails = selectedBookingId?.let { id ->
                    items.entries.find { it.key.id == id }?.value
                }

                _state.value = WooPosBookingsState.Content(
                    items = WooPosBookingsState.Content.Items.Loaded(items),
                    pullToRefreshState = WooPosPullToRefreshState.Enabled,
                    selectedDetails = selectedDetails,
                    paginationState = WooPosPaginationState.None,
                    dialogState = WooPosBookingsState.Content.DialogState.Hidden
                )
            }
        }
    }

    fun onBookingSelected(bookingId: Long) {
        selectedBookingId = bookingId
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        val items = (currentState.items as? WooPosBookingsState.Content.Items.Loaded) ?: return

        val updatedItems = items.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == bookingId)
        }
        val selectedDetails = updatedItems.entries
            .find { it.key.id == bookingId }
            ?.value

        _state.value = currentState.copy(
            items = WooPosBookingsState.Content.Items.Loaded(updatedItems),
            selectedDetails = selectedDetails
        )
    }

    fun onRefresh() {
        _state.value = when (val current = _state.value) {
            is WooPosBookingsState.Content -> current.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )
            else -> WooPosBookingsState.Loading
        }

        fetchJob?.cancel()
        loadMoreJob?.cancel()
        fetchJob = viewModelScope.launch {
            bookingListHandler.loadBookings(
                sortBy = BookingListSortOption.NewestToOldest
            ).onFailure {
                _state.value = when (val current = _state.value) {
                    is WooPosBookingsState.Content -> current.copy(
                        pullToRefreshState = WooPosPullToRefreshState.Enabled
                    )
                    else -> WooPosBookingsState.Error(
                        message = it.message ?: "Failed to load bookings"
                    )
                }
            }
        }
    }

    fun onEndOfBookingsListReached() {
        if (loadMoreJob?.isActive == true) return
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        if (currentState.paginationState is WooPosPaginationState.Error) return

        loadMoreJob = viewModelScope.launch {
            fetchJob?.join()

            val currentState = _state.value as? WooPosBookingsState.Content ?: return@launch
            _state.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

            bookingListHandler.loadMore()
                .onSuccess {
                    val updated = _state.value as? WooPosBookingsState.Content ?: return@launch
                    _state.value = updated.copy(paginationState = WooPosPaginationState.None)
                }
                .onFailure {
                    val updated = _state.value as? WooPosBookingsState.Content ?: return@launch
                    _state.value = updated.copy(
                        paginationState = WooPosPaginationState.Error
                    )
                }
        }
    }

    fun onPaginationErrorTryAgain() {
        loadMoreJob = viewModelScope.launch {
            val currentState = _state.value as? WooPosBookingsState.Content ?: return@launch
            _state.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

            val startTime = dateTimeProvider.now()
            val result = bookingListHandler.loadMore()
            val elapsed = dateTimeProvider.now() - startTime
            if (elapsed < MIN_LOADING_DURATION_MS) {
                delay(MIN_LOADING_DURATION_MS - elapsed)
            }

            result
                .onSuccess {
                    val updated = _state.value as? WooPosBookingsState.Content ?: return@launch
                    _state.value = updated.copy(paginationState = WooPosPaginationState.None)
                }
                .onFailure {
                    val updated = _state.value as? WooPosBookingsState.Content ?: return@launch
                    _state.value = updated.copy(paginationState = WooPosPaginationState.Error)
                }
        }
    }

    fun onBookingsLoadingErrorRetryButtonClicked() {
        _state.value = WooPosBookingsState.Loading
        fetchBookings()
    }

    fun onBookingsEmptyActionClicked() {
        _state.value = WooPosBookingsState.Loading
        fetchBookings()
    }

    fun onUIEvent(event: WooPosBookingsUIEvent) {
        when (event) {
            is WooPosBookingsUIEvent.BookingActionClicked -> handleBookingAction(event.action)
            is WooPosBookingsUIEvent.AttendanceToggled -> { }
            is WooPosBookingsUIEvent.PayByCardClicked -> { }
            is WooPosBookingsUIEvent.PayByCashClicked -> { }
            is WooPosBookingsUIEvent.AddBookingNoteClicked -> { }
            is WooPosBookingsUIEvent.CopyEmailClicked -> { }
        }
    }

    fun onIssueRefundDialogDismissed() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.Hidden
        )
    }

    private fun handleBookingAction(action: WooPosBookingsState.BookingAction) {
        when (action) {
            is WooPosBookingsState.BookingAction.EmailReceipt -> {
                // TBD: handle email receipt
            }
        }
    }

    private fun mapToItemViewState(booking: BookingEntity): WooPosBookingsState.BookingItemViewState {
        return WooPosBookingsState.BookingItemViewState(
            id = booking.id.value,
            title = booking.order.productInfo?.name ?: "#${booking.id.value}",
            date = booking.start.toString(),
            total = booking.order.paymentInfo?.total?.toPlainString() ?: "",
            customerEmail = booking.order.customerInfo?.billingEmail?.ifBlank { null },
            isSelected = booking.id.value == selectedBookingId,
            status = mapBookingStatus(booking.status),
            statusSlug = booking.status.key,
            createdAtMillis = booking.start.toEpochMilli()
        )
    }

    private fun mapToDetailsViewState(
        booking: BookingEntity
    ): WooPosBookingsState.BookingDetailsViewState {
        return WooPosBookingsState.BookingDetailsViewState(
            id = booking.id.value,
            number = "#${booking.id.value}",
            status = mapBookingStatus(booking.status),
            actionsState = WooPosBookingsState.BookingActionsState.Loaded(
                listOf(WooPosBookingsState.BookingAction.EmailReceipt(booking.orderId))
            ),
            headerTitle = "",
            headerSubtitle = "",
            attendanceBadge = null,
            bookingName = booking.order.productInfo?.name ?: "#${booking.id.value}",
            appointmentDate = "",
            appointmentTime = "",
            duration = "",
            teamMember = null,
            location = null,
            customerSection = null,
            attendanceSection = null,
            paymentSection = WooPosBookingsState.PaymentSection(
                serviceAmount = booking.order.paymentInfo?.subtotal?.toPlainString() ?: "-",
                taxAmount = booking.order.paymentInfo?.totalTax?.toPlainString() ?: "-",
                discountAmount = "-",
                totalAmount = booking.order.paymentInfo?.total?.toPlainString() ?: "-",
                paidWithLabel = booking.order.paymentInfo?.paymentMethodTitle,
                showPayButtons = false,
            ),
            bookingNote = null,
        )
    }

    private fun mapBookingStatus(status: BookingEntity.Status): WooPosBookingStatus {
        val colorKey = when (status) {
            BookingEntity.Status.Complete -> WooPosBookingStatusColorKey.COMPLETED
            BookingEntity.Status.Paid -> WooPosBookingStatusColorKey.COMPLETED
            BookingEntity.Status.Confirmed -> WooPosBookingStatusColorKey.PROCESSING
            BookingEntity.Status.PendingConfirmation -> WooPosBookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Unpaid -> WooPosBookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Cancelled -> WooPosBookingStatusColorKey.FAILED
            BookingEntity.Status.InCart -> WooPosBookingStatusColorKey.OTHER
            is BookingEntity.Status.Unknown -> WooPosBookingStatusColorKey.OTHER
        }
        return WooPosBookingStatus(
            text = status.key.replaceFirstChar { it.uppercase() }.replace("-", " "),
            colorKey = colorKey
        )
    }
}
