package com.woocommerce.android.ui.woopos.home.totals

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val orderDetailRepository: OrderDetailRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosTotalsState(
            orderId = null,
            isCollectPaymentButtonEnabled = false,
            orderSubtotal = BigDecimal.ZERO,
            orderTotal = BigDecimal.ZERO,
            orderTax = BigDecimal.ZERO,
            isLoading = true,
        ),
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
                }
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.OrderDraftCreated -> {
                        _state.value = state.value.copy(
                            orderId = event.orderId,
                            isCollectPaymentButtonEnabled = false,
                            isLoading = true
                        )
                        loadOrderDraft(event.orderId)
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
            calculateTotals(order)
        }
    }

    private fun calculateTotals(order: Order) {
        val subtotalAmount = order.items.sumOf { it.subtotal }
        val taxAmount = order.totalTax
        val totalAmount = subtotalAmount + taxAmount

        val updatedOrder = order.copy(
            total = totalAmount,
        )

        _state.value = _state.value.copy(
            orderId = updatedOrder.id,
            orderSubtotal = subtotalAmount,
            orderTotal = updatedOrder.total,
            orderTax = taxAmount,
            isCollectPaymentButtonEnabled = true,
            isLoading = false,
        )
    }
}
