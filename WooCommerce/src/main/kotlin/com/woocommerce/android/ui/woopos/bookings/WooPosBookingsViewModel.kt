package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.list.BookingListHandler
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.woopos.cardpayment.CardPaymentSource
import com.woocommerce.android.ui.woopos.common.util.WooPosClipboardHelper
import com.woocommerce.android.ui.woopos.common.util.isNetworkError
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.viewmodel.ResourceProvider
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val bookingListHandler: BookingListHandler,
    private val bookingsRepository: BookingsRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val mapper: WooPosBookingViewStateMapper,
    private val clipboardHelper: WooPosClipboardHelper,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    companion object {
        private const val MIN_LOADING_DURATION_MS = 300L
    }

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var selectedBookingId: Long? = null
    private var fetchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        observeBookings()
        fetchBookings()
        fetchResources()
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

    private fun fetchResources() {
        viewModelScope.launch {
            bookingsRepository.fetchResources()
        }
    }

    private fun observeBookings() {
        viewModelScope.launch {
            combine(
                bookingListHandler.bookingsFlow,
                bookingsRepository.observeResources()
            ) { bookings, resources ->
                bookings to resources.associateBy { it.id.value }
            }.collectLatest { (bookings, resourcesMap) ->
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
                    val resourceName = resourcesMap[booking.resourceId]?.name
                    mapper.mapToItemViewState(booking, selectedBookingId) to
                        mapper.mapToDetailsViewState(booking, resourceName)
                }

                val selectedDetails = selectedBookingId?.let { id ->
                    items.entries.find { it.key.id == id }?.value
                }

                val currentContentState = _state.value as? WooPosBookingsState.Content
                val currentPTRState = currentContentState
                    ?.pullToRefreshState
                    ?.takeIf { it == WooPosPullToRefreshState.Refreshing }
                    ?: WooPosPullToRefreshState.Enabled
                val paginationState = when (currentContentState?.paginationState) {
                    WooPosPaginationState.Loading,
                    WooPosPaginationState.Error -> WooPosPaginationState.None
                    else -> currentContentState?.paginationState ?: WooPosPaginationState.None
                }

                _state.value = WooPosBookingsState.Content(
                    items = WooPosBookingsState.Content.Items.Loaded(items),
                    pullToRefreshState = currentPTRState,
                    selectedDetails = selectedDetails,
                    paginationState = paginationState,
                    dialogState = currentContentState?.dialogState
                        ?: WooPosBookingsState.Content.DialogState.Hidden
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

    fun onPullToRefresh() {
        _state.value = when (val current = _state.value) {
            is WooPosBookingsState.Content -> current.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )
            else -> WooPosBookingsState.Loading
        }

        doRefresh()
    }

    private fun doRefresh() {
        fetchJob?.cancel()
        loadMoreJob?.cancel()
        fetchResources()
        fetchJob = viewModelScope.launch {
            bookingListHandler.loadBookings(
                sortBy = BookingListSortOption.NewestToOldest
            ).onSuccess {
                val current = _state.value
                if (current is WooPosBookingsState.Content) {
                    _state.value = current.copy(
                        pullToRefreshState = WooPosPullToRefreshState.Enabled
                    )
                }
            }.onFailure {
                when (val current = _state.value) {
                    is WooPosBookingsState.Content -> {
                        _state.value = current.copy(
                            pullToRefreshState = WooPosPullToRefreshState.Enabled
                        )
                        val messageResId = if (it.isNetworkError()) {
                            R.string.woo_pos_ptr_offline_error
                        } else {
                            R.string.something_went_wrong_try_again
                        }
                        _toastEvent.emit(
                            resourceProvider.getString(messageResId)
                        )
                    }
                    else -> {
                        _state.value = WooPosBookingsState.Error(
                            message = it.message ?: "Failed to load bookings"
                        )
                    }
                }
            }
        }
    }

    fun onEndOfBookingsListReached() {
        if (loadMoreJob?.isActive == true) return
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        if (currentState.paginationState is WooPosPaginationState.Error) return
        if (!bookingListHandler.hasMorePages) return

        loadMoreJob = viewModelScope.launch {
            fetchJob?.join()

            if (!bookingListHandler.hasMorePages) return@launch
            val currentState = _state.value as? WooPosBookingsState.Content ?: return@launch
            _state.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

            bookingListHandler.loadMore()
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
            is WooPosBookingsUIEvent.BookingMenuActionClicked -> handleBookingAction(event.action)
            is WooPosBookingsUIEvent.AttendanceToggled -> handleAttendanceToggle(event.attended)
            is WooPosBookingsUIEvent.CollectPaymentClicked -> handleCollectPayment()
            is WooPosBookingsUIEvent.AddBookingNoteClicked -> handleAddBookingNote()
            is WooPosBookingsUIEvent.CopyEmailClicked -> handleCopyToClipboard(event.email)
            is WooPosBookingsUIEvent.CopyPhoneClicked -> handleCopyToClipboard(event.phone)
            is WooPosBookingsUIEvent.CancelBookingConfirmed -> handleCancelConfirmed()
            is WooPosBookingsUIEvent.CancelBookingDismissed -> handleCancelDismissed()
        }
    }

    private fun handleCopyToClipboard(text: String) {
        clipboardHelper.copyToClipboard(text)
    }

    fun onIssueRefundDialogDismissed() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.Hidden
        )
        doRefresh()
    }

    private fun handleCollectPayment() {
        val details = (_state.value as? WooPosBookingsState.Content)?.selectedDetails ?: return
        viewModelScope.launch {
            _navigationEvent.emit(
                WooPosNavigationEvent.OpenCardPayment(
                    orderId = details.orderId,
                    source = CardPaymentSource.BOOKINGS,
                    showCashPaymentButton = true,
                )
            )
        }
    }

    private fun handleAttendanceToggle(attended: Boolean) {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        val details = currentState.selectedDetails ?: return
        val attendanceSection = details.attendanceSection ?: return

        val newAttendanceState = if (attended) {
            WooPosBookingsState.AttendanceState.ATTENDED
        } else {
            WooPosBookingsState.AttendanceState.UNATTENDED
        }

        val previousSelection = attendanceSection.selection
        val previousBadge = details.attendanceBadge

        val updatedDetails = details.copy(
            attendanceSection = attendanceSection.copy(selection = newAttendanceState),
            attendanceBadge = newAttendanceState,
        )
        _state.value = currentState.copy(
            selectedDetails = updatedDetails,
            items = updateItemsWithDetails(currentState.items, updatedDetails),
        )

        viewModelScope.launch {
            val entityStatus = if (attended) {
                BookingEntity.AttendanceStatus.Attended
            } else {
                BookingEntity.AttendanceStatus.Unattended
            }
            bookingsRepository.updateAttendanceStatus(
                bookingId = details.id,
                attendanceStatus = entityStatus,
            ).onFailure {
                val rollbackState = _state.value as? WooPosBookingsState.Content ?: return@onFailure
                val rollbackDetails = rollbackState.selectedDetails ?: return@onFailure
                if (rollbackDetails.id != details.id) return@onFailure
                val reverted = rollbackDetails.copy(
                    attendanceSection = rollbackDetails.attendanceSection?.copy(selection = previousSelection),
                    attendanceBadge = previousBadge,
                )
                _state.value = rollbackState.copy(
                    selectedDetails = reverted,
                    items = updateItemsWithDetails(rollbackState.items, reverted),
                )
            }
        }
    }

    private fun updateItemsWithDetails(
        items: WooPosBookingsState.Content.Items,
        updatedDetails: WooPosBookingsState.BookingDetailsViewState
    ): WooPosBookingsState.Content.Items {
        val loaded = items as? WooPosBookingsState.Content.Items.Loaded ?: return items
        val updatedMap = loaded.items.map { (item, details) ->
            if (item.id == updatedDetails.id) {
                item.copy(
                    attendanceBadge = updatedDetails.attendanceBadge ?: item.attendanceBadge
                ) to updatedDetails
            } else {
                item to details
            }
        }.toMap()
        return WooPosBookingsState.Content.Items.Loaded(updatedMap)
    }

    private fun handleAddBookingNote() {
        val bookingId = selectedBookingId ?: return
        viewModelScope.launch {
            _navigationEvent.emit(WooPosNavigationEvent.OpenBookingNote(bookingId))
        }
    }

    fun onBookingNoteSaved() {
        doRefresh()
    }

    fun onPaymentCompleted() {
        doRefresh()
    }

    private fun handleBookingAction(action: WooPosBookingsState.BookingAction) {
        when (action) {
            is WooPosBookingsState.BookingAction.ViewOrder -> {
                viewModelScope.launch {
                    _navigationEvent.emit(
                        WooPosNavigationEvent.OpenOrderDetails(orderId = action.orderId)
                    )
                }
            }
            is WooPosBookingsState.BookingAction.EmailReceipt -> {
                // TBD: handle email receipt
            }
            is WooPosBookingsState.BookingAction.IssueRefund -> {
                val currentState = _state.value as? WooPosBookingsState.Content ?: return
                _state.value = currentState.copy(
                    dialogState = WooPosBookingsState.Content.DialogState.IssueRefund(
                        orderId = action.orderId
                    )
                )
            }
            is WooPosBookingsState.BookingAction.CancelBooking -> {
                showCancelConfirmationDialog(action.bookingId)
            }
        }
    }

    private fun showCancelConfirmationDialog(bookingId: Long) {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        val details = currentState.selectedDetails ?: return

        val customerLabel = details.customerSection?.let {
            it.name?.takeIf(String::isNotBlank)
                ?: it.email?.takeIf(String::isNotBlank)
                ?: it.phone?.takeIf(String::isNotBlank)
        } ?: resourceProvider.getString(R.string.woopos_bookings_cancel_dialog_unknown_customer)

        val message = resourceProvider.getString(
            R.string.woopos_bookings_cancel_dialog_message,
            details.number.removePrefix("#"),
            details.bookingName,
            details.appointmentDate,
            details.appointmentTime,
            customerLabel
        )

        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.CancelBooking.PendingConfirmation(
                bookingId = bookingId,
                message = message,
            )
        )
    }

    private fun handleCancelConfirmed() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        val dialog = currentState.dialogState
            as? WooPosBookingsState.Content.DialogState.CancelBooking ?: return
        val bookingId = dialog.bookingId

        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.CancelBooking.Processing(
                bookingId = dialog.bookingId,
                message = dialog.message,
            )
        )

        viewModelScope.launch {
            val result = bookingsRepository.cancelBooking(bookingId)
            val state = _state.value as? WooPosBookingsState.Content ?: return@launch
            _state.value = if (result.isSuccess) {
                state.copy(
                    dialogState = WooPosBookingsState.Content.DialogState.Hidden
                )
            } else {
                state.copy(
                    dialogState = WooPosBookingsState.Content.DialogState.CancelBooking.Error(
                        bookingId = dialog.bookingId,
                        message = dialog.message,
                        errorMessage = resourceProvider.getString(
                            R.string.woopos_bookings_cancel_error
                        ),
                    )
                )
            }
            if (result.isSuccess) {
                doRefresh()
            }
        }
    }

    private fun handleCancelDismissed() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        if (currentState.dialogState is WooPosBookingsState.Content.DialogState.CancelBooking.Processing) return
        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.Hidden
        )
    }
}
