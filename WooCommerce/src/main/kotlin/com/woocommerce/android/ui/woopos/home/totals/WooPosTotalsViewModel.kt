package com.woocommerce.android.ui.woopos.home.totals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
import com.woocommerce.android.ui.woopos.common.composeui.component.snackbar.WooPosSnackbarState
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val orderDetailRepository: OrderDetailRepository,
    private val currencyFormatter: CurrencyFormatter,
    savedState: SavedStateHandle,
) : ViewModel() {
    private companion object {
        private const val EMPTY_ORDER_ID = -1L
        private val InitialState = WooPosTotalsState.Loading
    }

    private val _state = savedState.getStateFlow<WooPosTotalsState>(
        scope = viewModelScope,
        initialValue = InitialState,
        key = "totalsViewState"
    )

    val state: StateFlow<WooPosTotalsState> = _state

    private var orderId: MutableStateFlow<Long> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = EMPTY_ORDER_ID,
        key = "orderId",
    )

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            is WooPosTotalsUIEvent.CollectPaymentClicked -> {
                viewModelScope.launch {
                    val orderId = orderId.value
                    check(orderId != EMPTY_ORDER_ID)
                    val result = cardReaderFacade.collectPayment(orderId)
                    when (result) {
                        is WooPosCardReaderPaymentResult.Success -> {
                            val state = _state.value
                            check(state is WooPosTotalsState.Totals)
                            _state.value = WooPosTotalsState.PaymentSuccess
                            childrenToParentEventSender.sendToParent(ChildToParentEvent.OrderSuccessfullyPaid)
                        }
                        is WooPosCardReaderPaymentResult.Failure -> {
                            when (val state = state.value) {
                                is WooPosTotalsState.Totals -> {
                                    _state.value = state.copy(
                                        snackbar = WooPosSnackbarState.Triggered(
                                            R.string.woopos_payment_failed_please_try_again
                                        )
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
            WooPosTotalsUIEvent.SnackbarDismissed -> {
                when (val state = state.value) {
                    is WooPosTotalsState.Totals -> _state.value = state.copy(snackbar = WooPosSnackbarState.Hidden)
                    else -> Unit
                }
            }

            WooPosTotalsUIEvent.OnNewTransactionClicked -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(
                        ChildToParentEvent.NewTransactionClicked
                    )
                    _state.value = InitialState
                }
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.OrderDraftCreated -> {
                        orderId.value = event.orderId
                        _state.value = InitialState
                        loadOrderDraft(event.orderId)
                    }
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = InitialState
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun loadOrderDraft(orderId: Long) {
        viewModelScope.launch {
            val order = orderDetailRepository.getOrderById(orderId)
            check(order != null) { "Order must not be null" }
            check(order.items.isNotEmpty()) { "Order must have at least one item" }
            _state.value = calculateTotals(order)
        }
    }

    private fun calculateTotals(order: Order): WooPosTotalsState.Totals {
        val subtotalAmount = order.items.sumOf { it.subtotal }
        val taxAmount = order.totalTax
        val totalAmount = subtotalAmount + taxAmount

        val updatedOrder = order.copy(
            total = totalAmount,
        )

        return WooPosTotalsState.Totals(
            orderSubtotalText = currencyFormatter.formatCurrency(subtotalAmount.toPlainString()),
            orderTaxText = currencyFormatter.formatCurrency(taxAmount.toPlainString()),
            orderTotalText = currencyFormatter.formatCurrency(updatedOrder.total.toPlainString()),
        )
    }
}
