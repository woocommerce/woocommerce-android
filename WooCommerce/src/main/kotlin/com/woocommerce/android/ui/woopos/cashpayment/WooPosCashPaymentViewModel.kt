package com.woocommerce.android.ui.woopos.cashpayment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CashCollectPaymentSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosCashPaymentViewModel @Inject constructor(
    private val repository: WooPosCashPaymentRepository,
    private val priceFormat: WooPosFormatPrice,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId = savedState.get<Long>(CASH_ROUTE_ORDER_ID_KEY)!!

    private val _state = savedState.getStateFlow<WooPosCashPaymentState>(
        scope = viewModelScope,
        initialValue = WooPosCashPaymentState.Initiating,
        key = WOO_POS_CASH_PAYMENT_STATE_KEY
    )

    val state: StateFlow<WooPosCashPaymentState> = _state

    init {
        viewModelScope.launch {
            val savedStateValue = savedState.get<WooPosCashPaymentState>(WOO_POS_CASH_PAYMENT_STATE_KEY)
            if (savedStateValue != null && savedStateValue != WooPosCashPaymentState.Initiating) return@launch

            val order = repository.getOrderById(orderId)!!
            _state.value = WooPosCashPaymentState.Collecting(
                enteredAmount = null,
                changeDueText = "",
                total = order.total,
                totalText = resourceProvider.getString(
                    R.string.woopos_cash_payment_total,
                    priceFormat(order.total)
                ),
                errorMessage = null,
                currencySymbol = repository.getCurrencySymbol(),
                currencyPosition = repository.getCurrencySymbolPosition(),
                decimalSeparator = repository.getDecimalSeparator(),
                numberOfDecimals = repository.getNumberOfDecimals(),
                button = WooPosCashPaymentState.Collecting.Button(
                    text = resourceProvider.getString(R.string.woopos_complete_cash_order_button),
                    status = WooPosCashPaymentState.Collecting.Button.Status.DISABLED
                )
            )
        }
    }

    fun onUIEvent(event: WooPosCashPaymentUIEvent) {
        when (event) {
            is WooPosCashPaymentUIEvent.AmountChanged -> handleAmountChange(event)

            WooPosCashPaymentUIEvent.CompleteOrderClicked -> handleOrderCompletion()
        }
    }

    private fun handleAmountChange(event: WooPosCashPaymentUIEvent.AmountChanged) {
        viewModelScope.launch {
            val currentState = _state.value as? WooPosCashPaymentState.Collecting ?: return@launch
            val enteredAmount = event.newAmount ?: return@launch

            val changeDue = enteredAmount - currentState.total
            val changeDueText = if (changeDue >= BigDecimal.ZERO) {
                resourceProvider.getString(
                    R.string.woopos_cash_payment_change_due,
                    priceFormat(changeDue)
                )
            } else {
                ""
            }

            _state.value = currentState.copy(
                enteredAmount = enteredAmount,
                changeDueText = changeDueText,
                errorMessage = null,
                button = currentState.button.copy(
                    status = if (changeDue >= BigDecimal.ZERO) {
                        WooPosCashPaymentState.Collecting.Button.Status.ENABLED
                    } else {
                        WooPosCashPaymentState.Collecting.Button.Status.DISABLED
                    }
                )
            )
        }
    }

    private fun handleOrderCompletion() {
        viewModelScope.launch {
            val stateBeforeCompleting = _state.value as WooPosCashPaymentState.Collecting
            _state.value = stateBeforeCompleting.copy(
                button = stateBeforeCompleting.button.copy(
                    status = WooPosCashPaymentState.Collecting.Button.Status.LOADING
                )
            )

            val result = repository.completeOrder(orderId)
            if (result.isSuccess) {
                analyticsTracker.track(CashCollectPaymentSuccess)
                _state.value = WooPosCashPaymentState.Complete
            } else {
                val currentState = _state.value as? WooPosCashPaymentState.Collecting ?: return@launch
                _state.value = currentState.copy(
                    errorMessage = resourceProvider.getString(R.string.woopos_cash_payment_error_message),
                    button = currentState.button.copy(
                        status = WooPosCashPaymentState.Collecting.Button.Status.ENABLED
                    )
                )
            }
        }
    }

    private companion object {
        const val WOO_POS_CASH_PAYMENT_STATE_KEY = "woo_pos_cash_payment_state"
    }
}
