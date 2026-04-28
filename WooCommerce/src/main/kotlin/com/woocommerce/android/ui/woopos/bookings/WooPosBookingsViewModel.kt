package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.clock
import com.woocommerce.android.tools.SelectedSite
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Suppress("LargeClass")
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val bookingListHandler: BookingListHandler,
    private val bookingsRepository: BookingsRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val mapper: WooPosBookingViewStateMapper,
    private val clipboardHelper: WooPosClipboardHelper,
    private val resourceProvider: ResourceProvider,
    private val clock: Clock,
    private val analyticsTracker: WooPosBookingsAnalyticsTracker,
    selectedSite: SelectedSite,
) : ViewModel() {

    companion object {
        private const val MIN_LOADING_DURATION_MS = 300L
    }

    private val storeZoneId = selectedSite.get().clock.zone

    private var selectedBookingId: Long? = null
    private var selectedDate: LocalDate = Instant.now(clock).atZone(storeZoneId).toLocalDate()
    private var fetchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var attendanceUpdateJob: Job? = null
    private val locationCache = mutableMapOf<Long, String?>()

    private val _state = MutableStateFlow<WooPosBookingsState>(
        WooPosBookingsState.Loading(dateSelectorState = buildDateSelectorState())
    )
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

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
                filters = BookingFilters(dateRange = dateRangeForDate(selectedDate)),
                sortBy = BookingListSortOption.OldestToNewest
            )
            val current = _state.value
            result.onFailure { error ->
                when {
                    current is WooPosBookingsState.Loading -> {
                        _state.value = WooPosBookingsState.Error(
                            message = error.message ?: "Failed to load bookings"
                        )
                    }
                    current is WooPosBookingsState.Content -> {
                        _state.value = current.copy(
                            items = WooPosBookingsState.Content.Items.Error(
                                title = resourceProvider.getString(
                                    R.string.woopos_bookings_loading_error_title
                                ),
                                message = resourceProvider.getString(
                                    R.string.woopos_bookings_loading_error_message
                                )
                            ),
                            pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        )
                    }
                }
            }.onSuccess { fetchedCount ->
                if (fetchedCount > 0) return@onSuccess
                when {
                    current is WooPosBookingsState.Loading -> {
                        _state.value = buildNothingFoundState()
                    }
                    current is WooPosBookingsState.Content -> {
                        _state.value = current.copy(
                            items = WooPosBookingsState.Content.Items.NothingFound(
                                title = resourceProvider.getString(
                                    R.string.woopos_bookings_no_bookings_for_date
                                ),
                                message = resourceProvider.getString(
                                    R.string.woopos_bookings_no_bookings_for_date_message
                                )
                            ),
                            pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        )
                    }
                }
            }
        }
    }

    private fun fetchResources() {
        viewModelScope.launch {
            bookingsRepository.fetchResources()
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun observeBookings() {
        viewModelScope.launch {
            combine(
                bookingListHandler.bookingsFlow,
                bookingsRepository.observeResources()
            ) { bookings, resources ->
                bookings to resources.associateBy { it.id.value }
            }.collectLatest { (bookings, resourcesMap) ->
                val current = _state.value
                if (bookings.isEmpty() && current is WooPosBookingsState.Loading) {
                    return@collectLatest
                }

                if (bookings.isEmpty() && current is WooPosBookingsState.Content && fetchJob?.isActive == true) {
                    _state.value = current.copy(
                        items = WooPosBookingsState.Content.Items.Loading,
                    )
                    return@collectLatest
                }

                val dateSelectorState = buildDateSelectorState()

                if (bookings.isEmpty()) {
                    _state.value = WooPosBookingsState.Content(
                        items = WooPosBookingsState.Content.Items.NothingFound(
                            title = resourceProvider.getString(
                                R.string.woopos_bookings_no_bookings_for_date
                            ),
                            message = resourceProvider.getString(
                                R.string.woopos_bookings_no_bookings_for_date_message
                            )
                        ),
                        pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        dateSelectorState = dateSelectorState,
                        selectedDetails = null,
                        paginationState = WooPosPaginationState.None,
                        dialogState = WooPosBookingsState.Content.DialogState.Hidden
                    )
                    return@collectLatest
                }

                if (selectedBookingId == null) {
                    selectedBookingId = bookings.first().id.value
                }

                val items = bookings.associate { booking ->
                    val resource = resourcesMap[booking.resourceId]
                    mapper.mapToItemViewState(booking, selectedBookingId, resource) to
                        mapper.mapToDetailsViewState(
                            booking,
                            resource?.name,
                            locationCache[booking.productId]
                        )
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
                    dateSelectorState = dateSelectorState,
                    selectedDetails = selectedDetails,
                    paginationState = paginationState,
                    dialogState = currentContentState?.dialogState
                        ?: WooPosBookingsState.Content.DialogState.Hidden
                )

                val hasMissingLocations = bookings.any {
                    it.productId != 0L && it.productId !in locationCache
                }
                if (hasMissingLocations) {
                    fetchBookingLocations(bookings)
                    rebuildStateWithLocations(bookings, resourcesMap)
                }
            }
        }
    }

    fun onBookingSelected(bookingId: Long) {
        viewModelScope.launch { analyticsTracker.trackListItemTapped() }
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
            else -> WooPosBookingsState.Loading(dateSelectorState = buildDateSelectorState())
        }

        doRefresh()
    }

    private fun refreshSingleBooking(bookingId: Long) {
        viewModelScope.launch {
            bookingsRepository.fetchBooking(bookingId)
                .onFailure {
                    val messageResId = if (it.isNetworkError()) {
                        R.string.offline_error
                    } else {
                        R.string.something_went_wrong_try_again
                    }
                    _toastEvent.emit(resourceProvider.getString(messageResId))
                }
        }
    }

    private fun doRefresh() {
        fetchJob?.cancel()
        loadMoreJob?.cancel()
        fetchResources()
        fetchJob = viewModelScope.launch {
            bookingListHandler.loadBookings(
                filters = BookingFilters(dateRange = dateRangeForDate(selectedDate)),
                sortBy = BookingListSortOption.OldestToNewest
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
        _state.value = WooPosBookingsState.Loading(dateSelectorState = buildDateSelectorState())
        fetchBookings()
    }

    fun onBookingsEmptyActionClicked() {
        _state.value = WooPosBookingsState.Loading(dateSelectorState = buildDateSelectorState())
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
            is WooPosBookingsUIEvent.PreviousDayClicked -> {
                val newDate = selectedDate.minusDays(1)
                handleDateChange(newDate)
                viewModelScope.launch { analyticsTracker.trackDatePreviousTapped(newDate) }
            }
            is WooPosBookingsUIEvent.NextDayClicked -> {
                val newDate = selectedDate.plusDays(1)
                handleDateChange(newDate)
                viewModelScope.launch { analyticsTracker.trackDateNextTapped(newDate) }
            }
            is WooPosBookingsUIEvent.DateSelected -> {
                val newDate = Instant.ofEpochMilli(event.dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                handleDateChange(newDate)
                viewModelScope.launch { analyticsTracker.trackDateCalendarSelected(newDate) }
            }
        }
    }

    private fun handleCopyToClipboard(text: String) {
        clipboardHelper.copyToClipboard(text)
    }

    fun onBackFromIssueRefund() {
        if (_state.value !is WooPosBookingsState.Content) return
        selectedBookingId?.let { refreshSingleBooking(it) } ?: doRefresh()
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
        val attendanceSection = details.attendanceSection as? WooPosBookingsState.AttendanceSection.Visible ?: return

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

        attendanceUpdateJob?.cancel()
        attendanceUpdateJob = viewModelScope.launch {
            val entityStatus = if (attended) {
                BookingEntity.AttendanceStatus.Attended
            } else {
                BookingEntity.AttendanceStatus.Unattended
            }
            bookingsRepository.updateAttendanceStatus(
                bookingId = details.id,
                attendanceStatus = entityStatus,
            ).onSuccess {
                analyticsTracker.trackAttendanceChanged()
            }.onFailure { error ->
                analyticsTracker.trackAttendanceChangeFailed(this@WooPosBookingsViewModel::class, error)
                val rollbackState = _state.value as? WooPosBookingsState.Content ?: return@onFailure
                val rollbackDetails = rollbackState.selectedDetails ?: return@onFailure
                if (rollbackDetails.id != details.id) return@onFailure
                val rollbackAttendance = rollbackDetails.attendanceSection
                    as? WooPosBookingsState.AttendanceSection.Visible ?: return@onFailure
                val reverted = rollbackDetails.copy(
                    attendanceSection = rollbackAttendance.copy(selection = previousSelection),
                    attendanceBadge = previousBadge,
                )
                _state.value = rollbackState.copy(
                    selectedDetails = reverted,
                    items = updateItemsWithDetails(rollbackState.items, reverted),
                )
                _toastEvent.emit(resourceProvider.getString(R.string.booking_attendance_status_error))
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
            analyticsTracker.trackAddNoteTapped()
            _navigationEvent.emit(WooPosNavigationEvent.OpenBookingNote(bookingId))
        }
    }

    fun onBookingNoteSaved() {
        selectedBookingId?.let { refreshSingleBooking(it) } ?: doRefresh()
    }

    fun onPaymentCompleted() {
        hideCollectPaymentButton()
        selectedBookingId?.let { refreshSingleBooking(it) } ?: doRefresh()
    }

    private fun hideCollectPaymentButton() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        val selectedDetails = currentState.selectedDetails ?: return
        _state.value = currentState.copy(
            selectedDetails = selectedDetails.copy(
                paymentSection = selectedDetails.paymentSection.copy(
                    collectPaymentLabel = null
                )
            )
        )
    }

    private fun handleBookingAction(action: WooPosBookingsState.BookingAction) {
        when (action) {
            is WooPosBookingsState.BookingAction.ViewOrder -> {
                viewModelScope.launch {
                    analyticsTracker.trackViewOrderTapped()
                    _navigationEvent.emit(
                        WooPosNavigationEvent.OpenOrderDetails(orderId = action.orderId)
                    )
                }
            }
            is WooPosBookingsState.BookingAction.EmailReceipt -> {
                viewModelScope.launch {
                    _navigationEvent.emit(
                        WooPosNavigationEvent.OpenEmailReceipt(orderId = action.orderId)
                    )
                }
            }
            is WooPosBookingsState.BookingAction.IssueRefund -> {
                viewModelScope.launch {
                    analyticsTracker.trackIssueRefundTapped()
                    _navigationEvent.emit(
                        WooPosNavigationEvent.OpenIssueRefund(orderId = action.orderId)
                    )
                }
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
                analyticsTracker.trackBookingCancelled()
                state.copy(
                    dialogState = WooPosBookingsState.Content.DialogState.Hidden
                )
            } else {
                analyticsTracker.trackBookingCancelFailed(
                    this@WooPosBookingsViewModel::class,
                    result.exceptionOrNull() ?: Exception("Unknown error")
                )
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
                refreshSingleBooking(bookingId)
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

    private suspend fun fetchBookingLocations(bookings: List<BookingEntity>) {
        val newProductIds = bookings
            .map { it.productId }
            .distinct()
            .filter { it != 0L && it !in locationCache }
        if (newProductIds.isNotEmpty()) {
            coroutineScope {
                newProductIds.map { productId ->
                    async {
                        val location = bookingsRepository
                            .fetchProductBookingLocation(productId)
                            .getOrNull()
                        locationCache[productId] = location
                    }
                }.awaitAll()
            }
        }
    }

    private suspend fun rebuildStateWithLocations(
        bookings: List<BookingEntity>,
        resourcesMap: Map<Long, BookingResourceEntity>
    ) {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return

        val items = bookings.associate { booking ->
            val resource = resourcesMap[booking.resourceId]
            mapper.mapToItemViewState(booking, selectedBookingId, resource) to
                mapper.mapToDetailsViewState(
                    booking,
                    resource?.name,
                    locationCache[booking.productId]
                )
        }

        val selectedDetails = selectedBookingId?.let { id ->
            items.entries.find { it.key.id == id }?.value
        }

        _state.value = currentState.copy(
            items = WooPosBookingsState.Content.Items.Loaded(items),
            selectedDetails = selectedDetails,
        )
    }

    private fun handleDateChange(newDate: LocalDate) {
        selectedDate = newDate
        selectedBookingId = null
        _state.value = when (val current = _state.value) {
            is WooPosBookingsState.Content -> current.copy(
                dateSelectorState = buildDateSelectorState(),
                selectedDetails = null,
                pullToRefreshState = WooPosPullToRefreshState.Disabled
            )
            else -> WooPosBookingsState.Loading(dateSelectorState = buildDateSelectorState())
        }
        fetchBookings()
    }

    private fun buildNothingFoundState() = WooPosBookingsState.Content(
        items = WooPosBookingsState.Content.Items.NothingFound(
            title = resourceProvider.getString(
                R.string.woopos_bookings_no_bookings_for_date
            ),
            message = resourceProvider.getString(
                R.string.woopos_bookings_no_bookings_for_date_message
            )
        ),
        pullToRefreshState = WooPosPullToRefreshState.Enabled,
        dateSelectorState = buildDateSelectorState(),
        selectedDetails = null,
        paginationState = WooPosPaginationState.None,
        dialogState = WooPosBookingsState.Content.DialogState.Hidden
    )

    private fun buildDateSelectorState(): DateSelectorState {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, EEE", Locale.getDefault())
        val millis = selectedDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return DateSelectorState(
            formattedDate = selectedDate.format(formatter),
            selectedDateMillis = millis,
        )
    }

    private fun dateRangeForDate(date: LocalDate): BookingsFilterOption.DateRange {
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = date.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant()
        return BookingsFilterOption.DateRange(before = end, after = start)
    }
}
