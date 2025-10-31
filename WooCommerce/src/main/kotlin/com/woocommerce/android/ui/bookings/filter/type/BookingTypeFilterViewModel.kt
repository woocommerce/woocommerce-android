package com.woocommerce.android.ui.bookings.filter.type

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@HiltViewModel(assistedFactory = BookingTypeFilterViewModel.Factory::class)
class BookingTypeFilterViewModel @AssistedInject constructor(
    @Assisted private val initialType: BookingsFilterOption.BookingType,
    @Assisted private val onTypeFilterChanged: (BookingsFilterOption.BookingType) -> Unit,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingTypeFilterUiState(selectedType = initialType, onTypeSelected = ::onTypeSelected)
    )
    val uiState: StateFlow<BookingTypeFilterUiState> = _uiState

    private fun onTypeSelected(type: BookingsFilterOption.BookingType) {
        if (_uiState.value.selectedType != type) {
            _uiState.update { current -> current.copy(selectedType = type) }
        }
        onTypeFilterChanged(type)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initialType: BookingsFilterOption.BookingType,
            onTypeFilterChanged: (BookingsFilterOption.BookingType) -> Unit
        ): BookingTypeFilterViewModel
    }
}
