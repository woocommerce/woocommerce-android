package com.woocommerce.android.ui.bookings.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilter
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
            onShowBookings = ::onShowBookings
        )
    )
    val uiState = _uiState.asLiveData()

    init {
        getBookingFilter()
    }

    private fun getBookingFilter() {
        launch {
            // We don't observe changes here, just get the current value once
            val bookingFilter = bookingFilterRepository.bookingFilterFlow.firstOrNull()
            _uiState.update { current ->
                current.copy(initialBookingFilter = bookingFilter)
            }
        }
    }

    private fun onClose() {
        // TODO Verify unsaved changes and close
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun onShowBookings() {
        launch {
            bookingFilterRepository.save(_uiState.value.updatedBookingFilter)
        }
        triggerEvent(MultiLiveEvent.Event.Exit)
    }
}

private val BookingFilterListUiState.updatedBookingFilter: BookingFilter
    get() {
        val initial = initialBookingFilter ?: BookingFilter()
        val updates = updatedBookingFilters

        return BookingFilter(
            dateRange = updates.getOrDefault(initial.dateRange),
            customer = updates.getOrDefault(initial.customer),
            teamMember = updates.getOrDefault(initial.teamMember),
            attendanceStatus = updates.getOrDefault(initial.attendanceStatus),
            paymentStatus = updates.getOrDefault(initial.paymentStatus),
            bookingType = updates.getOrDefault(initial.bookingType),
            category = updates.getOrDefault(initial.category),
            serviceEvent = updates.getOrDefault(initial.serviceEvent),
        )
    }

private inline fun <reified T> Set<BookingsFilterOption>.getOrDefault(default: T?): T? {
    return this.filterIsInstance<T>().firstOrNull() ?: default
}
