package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cashpayment.CashPaymentSource
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val dataSource: WooPosBookingsDataSource,
    private val mapper: WooPosBookingMapper,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state

    private var currentTab = BookingTab.Today
    private var currentPage = 1
    private var bookings = mutableListOf<BookingDto>()
    private var selectedBookingId: Long? = null
    private var loadingJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        loadBookings()
    }

    fun onTabSelected(tab: BookingTab) {
        if (tab == currentTab) return
        currentTab = tab
        selectedBookingId = null

        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(
                selectedTab = tab,
                isLoadingList = true,
                selectedDetail = null,
            )
        }

        loadBookings()
    }

    fun onRefresh() {
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
        }
        loadBookings()
    }

    fun onBookingSelected(bookingId: Long) {
        selectedBookingId = bookingId
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            val booking = bookings.find { it.id == bookingId }
            _state.value = current.copy(
                items = current.items.map { it.copy(isSelected = it.id == bookingId) },
                selectedDetail = booking?.let {
                    mapper.toDetail(
                        dto = it,
                        customerName = dataSource.getCustomerName(it.customerId),
                        productName = dataSource.getProductName(it.productId),
                        orderTotals = orderTotalsFor(it),
                    )
                },
            )

            if (booking != null && booking.orderId != 0L && dataSource.getOrderTotals(booking.orderId) == null) {
                viewModelScope.launch {
                    dataSource.fetchOrderTotals(booking.orderId)
                    updateContentState()
                }
            }
        }
    }

    fun onEndOfListReached() {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        if (current.paginationState == WooPosPaginationState.Loading) return
        if (current.paginationState == WooPosPaginationState.None &&
            bookings.size < WooPosBookingsDataSource.PAGE_SIZE
        ) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _state.value = current.copy(paginationState = WooPosPaginationState.Loading)
            currentPage++
            dataSource.fetchBookings(currentTab, currentPage).fold(
                onSuccess = { result ->
                    bookings.addAll(result.bookings)
                    updateContentState()
                    dataSource.resolveNames(result.bookings)
                    updateContentState()
                },
                onFailure = {
                    currentPage--
                    val currentState = _state.value
                    if (currentState is WooPosBookingsState.Content) {
                        _state.value = currentState.copy(
                            paginationState = WooPosPaginationState.Error
                        )
                    }
                }
            )
        }
    }

    fun onAttendanceStatusSelected(status: AttendanceStatusUi) {
        val bookingId = selectedBookingId ?: return
        val entityStatus = when (status) {
            AttendanceStatusUi.Attended -> BookingEntity.AttendanceStatus.Attended
            AttendanceStatusUi.Unattended -> BookingEntity.AttendanceStatus.Unattended
            AttendanceStatusUi.Cancelled -> return
        }

        updateDetailLoadingState(attendanceUpdateInProgress = true)

        viewModelScope.launch {
            dataSource.updateAttendanceStatus(bookingId, entityStatus).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = {
                    updateDetailLoadingState(attendanceUpdateInProgress = false)
                }
            )
        }
    }

    fun onCancelBookingClicked() {
        val bookingId = selectedBookingId ?: return
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(
                dialogState = DialogState.CancelConfirmation(bookingId)
            )
        }
    }

    fun onCancelConfirmed() {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        val dialog = current.dialogState
        if (dialog !is DialogState.CancelConfirmation) return

        _state.value = current.copy(dialogState = DialogState.Hidden)
        updateDetailLoadingState(cancelInProgress = true)

        viewModelScope.launch {
            dataSource.cancelBooking(dialog.bookingId).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = {
                    updateDetailLoadingState(cancelInProgress = false)
                }
            )
        }
    }

    fun onCancelDialogDismissed() {
        val current = _state.value
        if (current is WooPosBookingsState.Content) {
            _state.value = current.copy(dialogState = DialogState.Hidden)
        }
    }

    fun onPayByCardClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        if (cardReaderFacade.readerStatus.value is CardReaderStatus.Connected) {
            navigateToCardPayment(detail.orderId)
        } else {
            cardReaderFacade.connectToReader()
            viewModelScope.launch {
                cardReaderFacade.readerStatus
                    .filter { it is CardReaderStatus.Connected }
                    .first()
                navigateToCardPayment(detail.orderId)
            }
        }
    }

    private fun navigateToCardPayment(orderId: Long) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToBookingCardPayment(orderId)
            )
        }
    }

    fun onPayByCashClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToCashPayment(
                    orderId = detail.orderId,
                    source = CashPaymentSource.BOOKINGS
                )
            )
        }
    }

    fun onViewOrderClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToOrderWithSearch(detail.orderId.toString())
            )
        }
    }

    fun onRetryClicked() {
        loadBookings()
    }

    fun onPaginationRetryClicked() {
        onEndOfListReached()
    }

    fun onReturnFromPayment() {
        val bookingId = selectedBookingId ?: return
        viewModelScope.launch {
            dataSource.markAsPaid(bookingId).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = {
                    dataSource.fetchBooking(bookingId).onSuccess { updated ->
                        replaceBookingInList(updated)
                        updateContentState()
                    }
                }
            )
        }
    }

    private fun loadBookings() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            currentPage = 1
            bookings.clear()

            if (_state.value !is WooPosBookingsState.Content) {
                _state.value = WooPosBookingsState.Loading
            }

            dataSource.fetchBookings(currentTab, currentPage).fold(
                onSuccess = { result ->
                    bookings.addAll(result.bookings)
                    if (bookings.isEmpty() && currentTab == BookingTab.All) {
                        _state.value = WooPosBookingsState.Empty
                    } else {
                        if (selectedBookingId == null && bookings.isNotEmpty()) {
                            selectedBookingId = bookings.first().id
                        }
                        updateContentState()
                        if (bookings.isNotEmpty()) {
                            dataSource.resolveNames(result.bookings)
                            fetchSelectedBookingOrderTotals()
                            updateContentState()
                        }
                    }
                },
                onFailure = {
                    _state.value = WooPosBookingsState.Error(
                        message = it.message ?: "Failed to load bookings"
                    )
                }
            )
        }
    }

    private fun updateContentState(
        paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ) {
        _state.value = WooPosBookingsState.Content(
            selectedTab = currentTab,
            items = bookings.map {
                mapper.toListItem(
                    dto = it,
                    selectedId = selectedBookingId,
                    customerName = dataSource.getCustomerName(it.customerId),
                    productName = dataSource.getProductName(it.productId),
                )
            },
            selectedDetail = bookings.find { it.id == selectedBookingId }?.let {
                mapper.toDetail(
                    dto = it,
                    customerName = dataSource.getCustomerName(it.customerId),
                    productName = dataSource.getProductName(it.productId),
                    orderTotals = orderTotalsFor(it),
                )
            },
            paginationState = paginationState,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            dialogState = DialogState.Hidden,
        )
    }

    private fun orderTotalsFor(booking: BookingDto): OrderTotalsData? {
        if (booking.orderId == 0L) return null
        return dataSource.getOrderTotals(booking.orderId)
    }

    private suspend fun fetchSelectedBookingOrderTotals() {
        val booking = bookings.find { it.id == selectedBookingId } ?: return
        if (booking.orderId != 0L) {
            dataSource.fetchOrderTotals(booking.orderId)
        }
    }

    private fun updateDetailLoadingState(
        attendanceUpdateInProgress: Boolean? = null,
        cancelInProgress: Boolean? = null,
    ) {
        val current = _state.value
        if (current !is WooPosBookingsState.Content) return
        val detail = current.selectedDetail ?: return
        _state.value = current.copy(
            selectedDetail = detail.copy(
                attendanceUpdateInProgress = attendanceUpdateInProgress
                    ?: detail.attendanceUpdateInProgress,
                cancelInProgress = cancelInProgress ?: detail.cancelInProgress,
            )
        )
    }

    private fun replaceBookingInList(updated: BookingDto) {
        val index = bookings.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            bookings[index] = updated
        }
    }
}
