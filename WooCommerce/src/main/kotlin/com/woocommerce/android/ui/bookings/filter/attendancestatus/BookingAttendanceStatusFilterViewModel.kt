package com.woocommerce.android.ui.bookings.filter.attendancestatus

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
            selectedStatus = initialStatuses?.values?.singleOrNull(),
            onStatusSelected = ::onStatusSelected
        )
    )
    val uiState: LiveData<BookingAttendanceStatusFilterUiState> = _uiState.asLiveData()

    private fun onStatusSelected(status: AttendanceStatus?) {
        val newSelectedStatus = when {
            status == AttendanceStatus.any -> null
            else -> status
        }

        _uiState.update { it.copy(selectedStatus = newSelectedStatus) }
        onFilterChanged(AttendanceStatuses(setOfNotNull(newSelectedStatus)))
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: AttendanceStatuses?,
            onFilterChanged: (AttendanceStatuses) -> Unit
        ): BookingAttendanceStatusFilterViewModel
    }
}
