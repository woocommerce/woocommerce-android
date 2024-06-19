package com.woocommerce.android.ui.woopos.home.totals

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
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
                    Log.d("WooPosTotalsViewModel", "Payment result: $result")
                    if (result is WooPosCardReaderPaymentResult.Success) {
                        // navigate to success screen
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
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.OrderDraftCreated -> {
                        _state.value = state.value.copy(
                            orderId = event.orderId,
                            isCollectPaymentButtonEnabled = true
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}
