package com.woocommerce.android.ui.woopos.markorderascomplete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCheckoutFromMarkAsPaid
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidConfirmed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidNotePostFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosMarkOrderAsCompleteViewModel @Inject constructor(
    private val repository: WooPosMarkOrderAsCompleteRepository,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val priceFormat: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedState[MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY])

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _state = savedState.getStateFlow<WooPosMarkOrderAsCompleteState>(
        scope = viewModelScope,
        initialValue = WooPosMarkOrderAsCompleteState.Initiating,
        key = STATE_KEY,
    )
    val state: StateFlow<WooPosMarkOrderAsCompleteState> = _state

    init {
        viewModelScope.launch {
            // If we were killed mid-confirm the button can come back as LOADING with no
            // running coroutine to flip it. Reset it so the user can retry instead of
            // staring at a permanently spinning button. The WC API is idempotent for
            // status transitions to Completed, so re-completing an already-completed
            // order is a no-op server-side; a duplicate non-customer note is the worst
            // outcome and is acceptable here.
            val current = _state.value
            if (current is WooPosMarkOrderAsCompleteState.Confirming &&
                current.button.status == WooPosMarkOrderAsCompleteState.Confirming.Button.Status.LOADING
            ) {
                _state.value = current.copy(
                    button = current.button.copy(
                        status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED,
                    ),
                )
                return@launch
            }
            if (current !is WooPosMarkOrderAsCompleteState.Initiating) return@launch

            val order = repository.getOrderById(orderId)
            val buttonText = resourceProvider.getString(R.string.woopos_mark_order_as_complete_confirm_button)
            _state.value = if (order != null) {
                WooPosMarkOrderAsCompleteState.Confirming(
                    totalText = resourceProvider.getString(
                        R.string.woopos_mark_order_as_complete_total,
                        priceFormat(order.total),
                    ),
                    note = "",
                    errorMessage = null,
                    button = WooPosMarkOrderAsCompleteState.Confirming.Button(
                        text = buttonText,
                        status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED,
                    ),
                )
            } else {
                WooPosMarkOrderAsCompleteState.Confirming(
                    totalText = "",
                    note = "",
                    errorMessage = resourceProvider.getString(
                        R.string.woopos_mark_order_as_complete_order_not_found,
                    ),
                    button = WooPosMarkOrderAsCompleteState.Confirming.Button(
                        text = buttonText,
                        status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.DISABLED,
                    ),
                )
            }
        }
    }

    fun onUIEvent(event: WooPosMarkOrderAsCompleteUIEvent) {
        when (event) {
            is WooPosMarkOrderAsCompleteUIEvent.NoteChanged -> handleNoteChanged(event.newNote)
            WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked -> handleConfirm()
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            analyticsTracker.track(BackToCheckoutFromMarkAsPaid)
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    private fun handleNoteChanged(newNote: String) {
        val current = _state.value as? WooPosMarkOrderAsCompleteState.Confirming ?: return
        _state.value = current.copy(note = newNote, errorMessage = null)
    }

    private fun handleConfirm() {
        viewModelScope.launch {
            val current = _state.value as? WooPosMarkOrderAsCompleteState.Confirming ?: return@launch
            if (current.button.status != WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED) return@launch
            analyticsTracker.track(MarkAsPaidConfirmed)
            _state.value = current.copy(
                button = current.button.copy(
                    status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.LOADING,
                ),
                errorMessage = null,
            )

            val outcome = repository.markOrderAsComplete(orderId, current.note.takeIf { it.isNotBlank() })
            when (outcome) {
                MarkOrderAsCompleteOutcome.SuccessWithFailedNote -> {
                    analyticsTracker.track(MarkAsPaidNotePostFailed)
                    onMarkAsPaidSucceeded()
                }
                MarkOrderAsCompleteOutcome.Success -> onMarkAsPaidSucceeded()
                MarkOrderAsCompleteOutcome.Failure -> {
                    analyticsTracker.track(MarkAsPaidFailed)
                    _state.value = current.copy(
                        errorMessage = resourceProvider.getString(R.string.woopos_mark_order_as_complete_error_message),
                        button = current.button.copy(
                            status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED,
                        ),
                    )
                }
            }
        }
    }

    private suspend fun onMarkAsPaidSucceeded() {
        analyticsTracker.track(MarkAsPaidSuccess)
        // Hand off to the home VM so it both flips the layout to full-screen totals
        // (hiding the cart pane) AND broadcasts OrderSuccessfullyPaid to the totals VM,
        // matching the card/cash success flows.
        childrenToParentEventSender.sendToParent(ChildToParentEvent.OrderSuccessfullyPaidExternally)
        _navigationEvent.emit(WooPosNavigationEvent.GoBack)
    }

    private companion object {
        const val STATE_KEY = "woo_pos_mark_order_as_complete_state"
    }
}
