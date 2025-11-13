package com.woocommerce.android.ui.bookings.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import javax.inject.Inject

@HiltViewModel
class BookingFilterListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingFilterRepository: BookingFilterRepository,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingFilterListUiState(
            onClose = ::onClose,
            onShowBookings = ::onShowBookings,
            openPage = ::onOpenPage,
            onUpdateFilterOption = ::onUpdateFilterOption
        )
    )
    val uiState = _uiState.asLiveData()

    init {
        getBookingFilter()
    }

    private fun onOpenPage(page: BookingFilterPage) {
        _uiState.update { current ->
            current.copy(currentPage = page)
        }
    }

    private fun onUpdateFilterOption(option: BookingsFilterOption) {
        _uiState.update { current ->
            val filtered = when (option) {
                is BookingsFilterOption.BookingType -> {
                    current.newBookingFilters.filterNot { it is BookingsFilterOption.BookingType }
                }

                else -> current.newBookingFilters.filterNot { it::class == option::class }
            }
            current.copy(
                newBookingFilters = filtered
                    .plus(option)
                    .toSet()
            )
        }
    }

    private fun getBookingFilter() {
        launch {
            // We don't observe changes here, just get the current value once
            val bookingFilters = bookingFilterRepository.bookingFiltersFlow.firstOrNull()
            _uiState.update { current ->
                current.copy(initialBookingFilters = bookingFilters)
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
        launch {
            bookingFilterRepository.save(_uiState.value.updatedBookingFilters)
        }
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun onDismissUnsavedChangesDialog() {
        _uiState.update { current -> current.copy(dialogState = null) }
    }

    private fun onDiscardChanges() {
        // Hide dialog and exit without saving
        _uiState.update { current -> current.copy(dialogState = null) }
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun hasUnsavedChanges(): Boolean {
        val initial = _uiState.value.initialBookingFilters ?: BookingFilters()
        val updated = _uiState.value.updatedBookingFilters
        return updated != initial
    }
}
