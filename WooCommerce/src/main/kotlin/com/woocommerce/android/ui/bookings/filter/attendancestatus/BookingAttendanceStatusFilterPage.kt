package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingAttendanceStatusFilterRoute(
    initialAttendanceStatus: BookingsFilterOption.AttendanceStatus?,
    onAttendanceStatusFilterChanged: (BookingsFilterOption.AttendanceStatus) -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingAttendanceStatusFilterViewModel, BookingAttendanceStatusFilterViewModel.Factory>
        { factory ->
            factory.create(initialAttendanceStatus, onAttendanceStatusFilterChanged)
        }
    val uiState by viewModel.uiState.observeAsState()
    uiState?.let { BookingAttendanceStatusFilterPage(it) }
}

@Composable
fun BookingAttendanceStatusFilterPage(state: BookingAttendanceStatusFilterUiState) {
    BookingsFilterSelectionPage(items = state.items)
}
