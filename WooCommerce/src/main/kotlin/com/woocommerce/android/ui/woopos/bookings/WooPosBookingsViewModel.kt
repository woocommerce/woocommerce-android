package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Suppress("LargeClass")
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    fun onRefresh() {
        return Unit
    }

    @Suppress("UnusedParameter")
    fun onBookingSelected(bookingId: Long) {
        return Unit
    }

    fun onEndOfBookingsListReached() {
        return Unit
    }

    fun onPaginationErrorTryAgain() {
        return Unit
    }

    @Suppress("UnusedParameter")
    fun onSearchEvent(event: WooPosSearchUIEvent) {
        return Unit
    }

    fun onSearchErrorRetry() {
        return Unit
    }

    fun onBookingsEmptyActionClicked() {
        return Unit
    }

    fun onBookingsLoadingErrorRetryButtonClicked() {
        return Unit
    }

    @Suppress("UnusedParameter")
    fun onUIEvent(event: WooPosBookingsUIEvent) {
        return Unit
    }

    fun onIssueRefundDialogDismissed() {
        return Unit
    }
}
