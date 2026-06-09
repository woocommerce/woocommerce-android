package com.woocommerce.android.ui.bookings.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.automattic.eventhorizon.BookingListApplyFiltersEvent
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import javax.inject.Inject

@HiltViewModel
class BookingFilterListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingFilterRepository: BookingFilterRepository,
    private val bookingsRepository: BookingsRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingFilterListUiState(
            onClose = ::onClose,
            onShowBookings = ::onShowBookings,
            openPage = ::onOpenPage,
            onUpdateFilterOption = ::onUpdateFilterOption,
            onClear = ::onClear
        )
    )
    val uiState = _uiState.asLiveData()

    init {
        getBookingFilter()
        observeSelectedTeamMembers()
    }

    private fun onOpenPage(page: BookingFilterPage) {
        _uiState.update { current ->
            current.copy(currentPage = page)
        }
    }

    private fun onUpdateFilterOption(option: BookingsFilterOption) {
        _uiState.update { current ->
            current.copy(
                updatedBookingFilters = current.updatedBookingFilters.updateFilterOption(option)
            )
        }
    }

    private fun onClear() {
        _uiState.update { it.copy(updatedBookingFilters = BookingFilters()) }
    }

    private fun getBookingFilter() {
        launch {
            // We don't observe changes here, just get the current value once
            val bookingFilters = bookingFilterRepository.bookingFiltersFlow.firstOrNull() ?: BookingFilters()
            _uiState.update { current ->
                current.copy(initialBookingFilters = bookingFilters, updatedBookingFilters = bookingFilters)
            }
        }
    }

    private fun observeSelectedTeamMembers() {
        launch {
            _uiState
                .map { state -> state.updatedBookingFilters.teamMembers.values.map { it.value } }
                .distinctUntilChanged()
                .collect { ids ->
                    val teamMemberValue = when {
                        ids.isEmpty() -> null
                        ids.size == 1 -> bookingsRepository.getResource(ids.first())?.name
                        else -> ids.size.toString()
                    }
                    _uiState.update { current ->
                        current.copy(
                            selectedFilterValues = current.selectedFilterValues.copy(teamMemberValue = teamMemberValue)
                        )
                    }
                }
        }
    }

    private fun onClose() {
        if (_uiState.value.currentPage != BookingFilterPage.List) {
            _uiState.update { current ->
                current.copy(currentPage = BookingFilterPage.List)
            }
        } else {
            if (hasUnsavedChanges()) {
                _uiState.update { current ->
                    current.copy(
                        dialogState = DialogState(
                            message = R.string.discard_message,
                            positiveButton = DialogState.DialogButton(
                                text = UiString.UiStringRes(R.string.discard),
                                onClick = ::onDiscardChanges
                            ),
                            negativeButton = DialogState.DialogButton(
                                text = UiString.UiStringRes(R.string.keep_changes),
                                onClick = ::onDismissUnsavedChangesDialog
                            ),
                        )
                    )
                }
            } else {
                triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    private fun onShowBookings() {
        val filters = _uiState.value.updatedBookingFilters
        analyticsTrackerWrapper.track(
            BookingListApplyFiltersEvent(
                selectedFilters = filters.activeFilterTrackingKeys().sorted().toString()
            )
        )
        launch {
            bookingFilterRepository.save(filters)
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    private fun onDismissUnsavedChangesDialog() {
        _uiState.update { current -> current.copy(dialogState = null) }
    }

    private fun onDiscardChanges() {
        // Hide dialog and exit without saving
        _uiState.update { current -> current.copy(dialogState = null) }
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun BookingFilters.activeFilterTrackingKeys(): List<String> = buildList {
        if (teamMembers != BookingsFilterOption.TeamMembers.DEFAULT) add("team_member")
        if (bookingType?.value != null) add("booking_type")
        if (serviceEvents != BookingsFilterOption.ServiceEvents.DEFAULT) add("service_events")
        if (attendanceStatus != BookingsFilterOption.AttendanceStatus.DEFAULT) add("attendance_status")
        if (paymentStatus != null) add("payment_status")
        if (customer != null) add("customer")
        if (dateRange != BookingsFilterOption.DateRange.DEFAULT) add("date_time")
        if (location != null) add("location")
    }

    private fun hasUnsavedChanges() = _uiState.value.initialBookingFilters != _uiState.value.updatedBookingFilters
}
