package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val dataSource: WooPosBookingsDataSource,
    private val mapper: WooPosBookingMapper,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
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
            _state.value = current.copy(
                items = current.items.map { it.copy(isSelected = it.id == bookingId) },
                selectedDetail = bookings.find { it.id == bookingId }?.let {
                    mapper.toDetail(it)
                },
            )
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
            AttendanceStatusUi.Booked -> BookingEntity.AttendanceStatus.Booked
            AttendanceStatusUi.CheckedIn -> BookingEntity.AttendanceStatus.CheckedIn
            AttendanceStatusUi.NoShow -> BookingEntity.AttendanceStatus.NoShow
            AttendanceStatusUi.Cancelled -> BookingEntity.AttendanceStatus.Cancelled
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
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToBookingCardPayment(detail.orderId)
            )
        }
    }

    fun onPayByCashClicked() {
        val detail = (_state.value as? WooPosBookingsState.Content)?.selectedDetail ?: return
        if (detail.orderId == 0L) return
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.NavigationEvent.ToCashPayment(detail.orderId)
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

    fun onReturnFromCashPayment(bookingId: Long) {
        viewModelScope.launch {
            dataSource.markAsPaid(bookingId).fold(
                onSuccess = { updated ->
                    replaceBookingInList(updated)
                    updateContentState()
                },
                onFailure = { }
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
                    if (bookings.isEmpty()) {
                        _state.value = WooPosBookingsState.Empty
                    } else {
                        if (selectedBookingId == null) {
                            selectedBookingId = bookings.first().id
                        }
                        updateContentState()
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
            items = bookings.map { mapper.toListItem(it, selectedBookingId) },
            selectedDetail = bookings.find { it.id == selectedBookingId }?.let {
                mapper.toDetail(it)
            },
            paginationState = paginationState,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            dialogState = DialogState.Hidden,
        )
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
