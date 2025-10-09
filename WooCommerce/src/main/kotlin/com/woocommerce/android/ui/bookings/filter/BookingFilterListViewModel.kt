package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import javax.inject.Inject

@HiltViewModel
class BookingFilterListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingFilterListUiState(
            items = defaultBookingFilters().map { BookingFilterListItem(title = it.titleRes()) },
            onClose = ::onClose,
            onShowBookings = ::onShowBookings
        )
    )
    val uiState = _uiState.asLiveData()

    private fun onClose() {
        // TODO Verify unsaved changes and close
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun onShowBookings() {
        // TODO Apply filters and show bookings
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    @StringRes
    private fun BookingsFilterOption.titleRes(): Int = when (this) {
        BookingsFilterOption.TeamMember -> R.string.bookings_filter_title_team_member
        BookingsFilterOption.AttendanceStatus -> R.string.bookings_filter_title_attendance_status
        BookingsFilterOption.PaymentStatus -> R.string.bookings_filter_title_payment_status
        BookingsFilterOption.BookingType -> R.string.bookings_filter_title_type
        is BookingsFilterOption.Customer -> R.string.bookings_filter_customer_name
        BookingsFilterOption.Category -> R.string.bookings_filter_category
        is BookingsFilterOption.DateRange -> R.string.bookings_filter_title_date
        BookingsFilterOption.ServiceEvent -> R.string.bookings_filter_title_service_event
    }

    private fun defaultBookingFilters(): List<BookingsFilterOption> = listOf(
        BookingsFilterOption.TeamMember,
        BookingsFilterOption.AttendanceStatus,
        BookingsFilterOption.PaymentStatus,
        BookingsFilterOption.BookingType,
        BookingsFilterOption.Customer(customerId = null),
        BookingsFilterOption.Category,
        BookingsFilterOption.DateRange(before = null, after = null),
        BookingsFilterOption.ServiceEvent,
    )

    data class BookingFilterListItem(
        @StringRes val title: Int,
        val value: String? = null,
        val onClick: () -> Unit = {}
    )

    data class BookingFilterListUiState(
        val items: List<BookingFilterListItem>,
        val onClose: () -> Unit = {},
        val onShowBookings: () -> Unit = {},
    )
}
