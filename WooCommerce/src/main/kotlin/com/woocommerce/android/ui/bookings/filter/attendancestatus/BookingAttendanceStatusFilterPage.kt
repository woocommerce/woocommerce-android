package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingAttendanceStatusFilterRoute(
    initialAttendanceStatuses: BookingsFilterOption.AttendanceStatuses?,
    onAttendanceStatusesFilterChanged: (BookingsFilterOption.AttendanceStatuses) -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingAttendanceStatusFilterViewModel, BookingAttendanceStatusFilterViewModel.Factory>
        { factory ->
            factory.create(initialAttendanceStatuses, onAttendanceStatusesFilterChanged)
        }
    val uiState by viewModel.uiState.collectAsState()
    BookingAttendanceStatusFilterPage(uiState)
}

@Composable
fun BookingAttendanceStatusFilterPage(state: BookingAttendanceStatusFilterUiState) {
    BookingsFilterSelectionPage(items = state.items)
}
