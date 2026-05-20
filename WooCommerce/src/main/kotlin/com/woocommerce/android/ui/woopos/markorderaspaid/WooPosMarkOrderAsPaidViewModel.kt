package com.woocommerce.android.ui.woopos.markorderaspaid

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
class WooPosMarkOrderAsPaidViewModel @Inject constructor(
    private val repository: WooPosMarkOrderAsPaidRepository,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val priceFormat: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedState[MARK_ORDER_AS_PAID_ROUTE_ORDER_ID_KEY]) {
        "orderId missing in MarkOrderAsPaid args"
    }

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _state = savedState.getStateFlow<WooPosMarkOrderAsPaidState>(
        scope = viewModelScope,
        initialValue = WooPosMarkOrderAsPaidState.Initiating,
        key = STATE_KEY,
    )
    val state: StateFlow<WooPosMarkOrderAsPaidState> = _state

    init {
        viewModelScope.launch {
            when (val current = _state.value) {
                is WooPosMarkOrderAsPaidState.Confirming -> resetStaleProcessing(current)
                WooPosMarkOrderAsPaidState.Initiating -> loadOrder()
            }
        }
    }

    private fun resetStaleProcessing(current: WooPosMarkOrderAsPaidState.Confirming) {
        if (current.isProcessing) {
            _state.value = current.copy(isProcessing = false)
        }
    }

    private suspend fun loadOrder() {
        val order = repository.getOrderById(orderId)
        _state.value = if (order != null) {
            WooPosMarkOrderAsPaidState.Confirming(
                formattedTotal = priceFormat(order.total),
                note = "",
                errorMessage = null,
                isProcessing = false,
                canConfirm = true,
            )
        } else {
            WooPosMarkOrderAsPaidState.Confirming(
                formattedTotal = "",
                note = "",
                errorMessage = resourceProvider.getString(
                    R.string.woopos_mark_order_as_paid_order_not_found,
                ),
                isProcessing = false,
                canConfirm = false,
            )
        }
    }

    fun onUIEvent(event: WooPosMarkOrderAsPaidUIEvent) {
        when (event) {
            is WooPosMarkOrderAsPaidUIEvent.NoteChanged -> handleNoteChanged(event.newNote)
            WooPosMarkOrderAsPaidUIEvent.ConfirmClicked -> handleConfirm()
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            analyticsTracker.track(BackToCheckoutFromMarkAsPaid)
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    private fun handleNoteChanged(newNote: String) {
        val current = _state.value as? WooPosMarkOrderAsPaidState.Confirming ?: return
        val errorMessage = if (current.canConfirm) null else current.errorMessage
        _state.value = current.copy(note = newNote, errorMessage = errorMessage)
    }

    private fun handleConfirm() {
        viewModelScope.launch {
            val current = _state.value as? WooPosMarkOrderAsPaidState.Confirming ?: return@launch
            if (!current.canConfirm || current.isProcessing) return@launch
            analyticsTracker.track(MarkAsPaidConfirmed)
            _state.value = current.copy(isProcessing = true, errorMessage = null)

            val outcome = repository.markOrderAsPaid(orderId, current.note.takeIf { it.isNotBlank() })
            when (outcome) {
                MarkOrderAsPaidOutcome.SuccessWithFailedNote -> {
                    analyticsTracker.track(MarkAsPaidNotePostFailed)
                    onMarkAsPaidSucceeded()
                }
                MarkOrderAsPaidOutcome.Success -> onMarkAsPaidSucceeded()
                MarkOrderAsPaidOutcome.Failure -> {
                    analyticsTracker.track(MarkAsPaidFailed)
                    _state.value = current.copy(
                        errorMessage = resourceProvider.getString(R.string.woopos_mark_order_as_paid_error_message),
                        isProcessing = false,
                    )
                }
            }
        }
    }

    private suspend fun onMarkAsPaidSucceeded() {
        analyticsTracker.track(MarkAsPaidSuccess)
        childrenToParentEventSender.sendToParent(ChildToParentEvent.OrderSuccessfullyPaidExternally)
        _navigationEvent.emit(WooPosNavigationEvent.GoBack)
    }

    private companion object {
        const val STATE_KEY = "woo_pos_mark_order_as_paid_state"
    }
}
