package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.AttendanceStatus

@HiltViewModel(assistedFactory = BookingAttendanceStatusFilterViewModel.Factory::class)
class BookingAttendanceStatusFilterViewModel @AssistedInject constructor(
    @Assisted private val initialStatus: AttendanceStatus?,
    @Assisted private val onFilterChanged: (AttendanceStatus) -> Unit,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingAttendanceStatusFilterUiState(
            selectedStatus = initialStatus ?: AttendanceStatus(null),
            onStatusSelected = ::onStatusSelected
        )
    )
    val uiState: StateFlow<BookingAttendanceStatusFilterUiState> = _uiState

    private fun onStatusSelected(status: AttendanceStatus) {
        if (_uiState.value.selectedStatus != status) {
            _uiState.update { it.copy(selectedStatus = status) }
        }
        onFilterChanged(status)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: AttendanceStatus?,
            onFilterChanged: (AttendanceStatus) -> Unit
        ): BookingAttendanceStatusFilterViewModel
    }
}
