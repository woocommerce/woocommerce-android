package com.woocommerce.android.ui.bookings.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    resourceProvider: ResourceProvider,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingDetailsFragmentArgs by savedState.navArgs()

    private val _state = MutableStateFlow(BookingDetailsViewState())
    val state: StateFlow<BookingDetailsViewState> = _state

    init {
        _state.update {
            it.copy(
                toolbarTitle = resourceProvider.getString(R.string.booking_details_title, navArgs.bookingId)
            )
        }
    }
}
