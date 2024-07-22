package com.woocommerce.android.ui.woopos.home.totals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
    private val totalsRepository: WooPosTotalsRepository,
    private val priceFormat: WooPosFormatPrice,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private companion object {
        private const val EMPTY_ORDER_ID = -1L
        private const val KEY_PRODUCT_IDS = "productIds"
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

    private var productIds: List<Long>
        get() = savedState.get<List<Long>>(KEY_PRODUCT_IDS) ?: emptyList()
        set(value) = savedState.set(KEY_PRODUCT_IDS, value)
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
                            _state.value = WooPosTotalsState.PaymentSuccess(
                                state.orderSubtotalText,
                                state.orderTaxText,
                                state.orderTotalText
                            )
                            childrenToParentEventSender.sendToParent(ChildToParentEvent.OrderSuccessfullyPaid)
                        }

                        else -> Unit
                    }
                }
            }

            is WooPosTotalsUIEvent.OnNewTransactionClicked -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(
                        ChildToParentEvent.NewTransactionClicked
                    )
                    _state.value = InitialState
                }
            }

            is WooPosTotalsUIEvent.RetryOrderCreationClicked -> {
                viewModelScope.launch {
                    _state.value = WooPosTotalsState.Loading
                    attemptCreateOrderAgain()
                }
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.CheckoutClicked -> {
                        productIds = event.productIds
                        createOrderDraft(productIds)
                    }

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        _state.value = InitialState
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun attemptCreateOrderAgain() {
        createOrderDraft(productIds)
    }

    private fun createOrderDraft(productIds: List<Long>) {
        viewModelScope.launch {
            _state.value = WooPosTotalsState.Loading

            totalsRepository.createOrderWithProducts(productIds = productIds)
                .fold(
                    onSuccess = { order ->
                        orderId.value = order.id
                        _state.value = buildTotalsState(order)
                    },
                    onFailure = { error ->
                        WooLog.e(T.ORDERS, "Order creation failed - $error")
                        _state.value = WooPosTotalsState.Error(error.message ?: "Unknown error")
                    }
                )
        }
    }

    private suspend fun buildTotalsState(order: Order): WooPosTotalsState.Totals {
        val subtotalAmount = order.items.sumOf { it.subtotal }
        val taxAmount = order.totalTax
        val totalAmount = subtotalAmount + taxAmount

        return WooPosTotalsState.Totals(
            orderSubtotalText = priceFormat(subtotalAmount),
            orderTaxText = priceFormat(taxAmount),
            orderTotalText = priceFormat(totalAmount),
        )
    }
}
