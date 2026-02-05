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

    fun onRefresh() {}

    fun onBookingSelected(bookingId: Long) {}

    fun onEndOfBookingsListReached() {}

    fun onPaginationErrorTryAgain() {}

    fun onSearchEvent(event: WooPosSearchUIEvent) {}

    fun onSearchErrorRetry() {}

    fun onBookingsEmptyActionClicked() {}

    fun onBookingsLoadingErrorRetryButtonClicked() {}

    fun onUIEvent(event: WooPosBookingsUIEvent) {}

    fun onIssueRefundDialogDismissed() {}
}
