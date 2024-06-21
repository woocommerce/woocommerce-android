package com.woocommerce.android.ui.woopos.home.totals

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OrderSuccessfullyPaid
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val cardReaderFacade: WooPosCardReaderFacade,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosTotalsState(orderId = null, isCollectPaymentButtonEnabled = false),
        key = "totalsViewState"
    )
    val state: StateFlow<WooPosTotalsState> = _state

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            is WooPosTotalsUIEvent.CollectPaymentClicked -> {
                viewModelScope.launch {
                    val orderId = state.value.orderId!!
                    val result = cardReaderFacade.collectPayment(orderId)
                    if (result is WooPosCardReaderPaymentResult.Success) {
                        // navigate to success screen
                        childrenToParentEventSender.sendToParent(OrderSuccessfullyPaid(orderId))
                    } else {
                        _state.value = state.value.copy(
                            snackbarMessage = SnackbarMessage.Triggered(
                                R.string.woopos_payment_failed_please_try_again
                            )
                        )
                    }
                }
            }

            WooPosTotalsUIEvent.SnackbarDismissed ->
                _state.value =
                    state.value.copy(snackbarMessage = SnackbarMessage.Hidden)
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            Log.d("WooPos", "WooPosTotalsViewModel.listenEventsFromParent() $this")
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.OrderDraftCreated -> {
                        _state.value = state.value.copy(
                            orderId = event.orderId,
                            isCollectPaymentButtonEnabled = true
                        )
                    }
                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        _state.value = WooPosTotalsState(orderId = null, isCollectPaymentButtonEnabled = false)
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        Log.d("WooPos", "WooPosTotalsViewModel.onCleared() $this")
    }
}
