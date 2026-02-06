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
        fetchBookings()
        observeBookings()
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
                    val details = items.entries.find { it.key.id == id }?.value
                    (details as? WooPosBookingsState.BookingDetailsViewState.Computed)?.details
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
            ?.let { (it as? WooPosBookingsState.BookingDetailsViewState.Computed)?.details }

        _state.value = currentState.copy(
            items = WooPosBookingsState.Content.Items.Loaded(updatedItems),
            selectedDetails = selectedDetails
        )
    }

    fun onEndOfBookingsListReached() {
        return Unit
    }

    fun onPaginationErrorTryAgain() {
        return Unit
    }

    fun onBookingsEmptyActionClicked() {
        return Unit
    }

    fun onBookingsLoadingErrorRetryButtonClicked() {
        return Unit
    }

    @Suppress("UnusedParameter")
    fun onUIEvent(event: WooPosBookingsUIEvent) {
        return Unit
    }

    fun onIssueRefundDialogDismissed() {
        return Unit
    }

    private fun mapToItemViewState(booking: BookingEntity): WooPosBookingsState.BookingItemViewState {
        // TODO: create a POS-specific mapper for BookingEntity -> BookingItemViewState
        return WooPosBookingsState.BookingItemViewState(
            id = booking.id.value,
            title = booking.order.productInfo?.name ?: "#${booking.id.value}",
            date = booking.start.toString(),
            total = booking.order.paymentInfo?.total?.toPlainString() ?: "",
            customerEmail = booking.order.customerInfo?.billingEmail,
            isSelected = booking.id.value == selectedBookingId,
            status = mapBookingStatus(booking.status),
            statusSlug = booking.status.key,
            createdAtMillis = booking.start.toEpochMilli()
        )
    }

    private fun mapToDetailsViewState(
        booking: BookingEntity
    ): WooPosBookingsState.BookingDetailsViewState {
        // TODO: create a POS-specific mapper for BookingEntity -> BookingDetailsViewState
        return WooPosBookingsState.BookingDetailsViewState.Computed(
            orderId = booking.orderId,
            details = WooPosBookingsState.BookingDetailsViewState.Computed.Details(
                id = booking.id.value,
                number = "#${booking.id.value}",
                dateTime = booking.start.toString(),
                customerEmail = booking.order.customerInfo?.billingEmail,
                status = mapBookingStatus(booking.status),
                lineItems = emptyList(),
                breakdown = WooPosBookingsState.BookingDetailsViewState.Computed.Details.TotalsBreakdown(
                    products = booking.order.paymentInfo?.subtotal?.toPlainString() ?: "",
                    discount = null,
                    discountCode = null,
                    taxes = booking.order.paymentInfo?.totalTax?.toPlainString() ?: "",
                    shipping = null,
                    refunds = emptyList(),
                    netPayment = null
                ),
                total = booking.order.paymentInfo?.total?.toPlainString() ?: "",
                totalPaid = booking.order.paymentInfo?.total?.toPlainString() ?: "",
                paymentMethodTitle = booking.order.paymentInfo?.paymentMethodTitle,
                actionsState = WooPosBookingsState.BookingActionsState.Loaded(
                    listOf(WooPosBookingsState.BookingAction.EmailReceipt(booking.orderId))
                )
            )
        )
    }

    private fun mapBookingStatus(status: BookingEntity.Status): PosBookingStatus {
        val colorKey = when (status) {
            BookingEntity.Status.Complete -> BookingStatusColorKey.COMPLETED
            BookingEntity.Status.Paid -> BookingStatusColorKey.COMPLETED
            BookingEntity.Status.Confirmed -> BookingStatusColorKey.PROCESSING
            BookingEntity.Status.PendingConfirmation -> BookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Unpaid -> BookingStatusColorKey.ON_HOLD
            BookingEntity.Status.Cancelled -> BookingStatusColorKey.FAILED
            BookingEntity.Status.InCart -> BookingStatusColorKey.OTHER
            is BookingEntity.Status.Unknown -> BookingStatusColorKey.OTHER
        }
        return PosBookingStatus(
            text = status.key.replaceFirstChar { it.uppercase() }.replace("-", " "),
            colorKey = colorKey
        )
    }
}
