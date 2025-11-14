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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.AttendanceStatuses
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus

@HiltViewModel(assistedFactory = BookingAttendanceStatusFilterViewModel.Factory::class)
class BookingAttendanceStatusFilterViewModel @AssistedInject constructor(
    @Assisted private val initialStatuses: AttendanceStatuses?,
    @Assisted private val onFilterChanged: (AttendanceStatuses) -> Unit,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingAttendanceStatusFilterUiState(
            selectedStatuses = initialStatuses ?: AttendanceStatuses.DEFAULT,
            onStatusSelected = ::onStatusSelected
        )
    )
    val uiState: StateFlow<BookingAttendanceStatusFilterUiState> = _uiState

    private fun onStatusSelected(status: AttendanceStatus?) {
        val newSelectedStatusesState = if (status == AttendanceStatus.any) {
            AttendanceStatuses.DEFAULT
        } else {
            val statusSet = _uiState.value.selectedStatuses.values.toMutableSet()
            if (statusSet.contains(status)) {
                statusSet.remove(status)
            } else {
                status?.let { statusSet.add(it) }
            }
            AttendanceStatuses(statusSet)
        }

        _uiState.update { it.copy(selectedStatuses = newSelectedStatusesState) }
        onFilterChanged(newSelectedStatusesState)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: AttendanceStatuses?,
            onFilterChanged: (AttendanceStatuses) -> Unit
        ): BookingAttendanceStatusFilterViewModel
    }
}
