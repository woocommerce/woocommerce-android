package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
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
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val totalsRepository: WooPosTotalsRepository,
    private val priceFormat: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
        private const val DEBOUNCE_TIME_MS = 3000L
        private const val KEY_STATE = "woo_pos_totals_data_state"
        private val InitialState = WooPosTotalsViewState.Loading
    }

    private val uiState = savedState.getStateFlow<WooPosTotalsViewState>(
        scope = viewModelScope,
        initialValue = InitialState,
        key = "woo_pos_totals_view_state"
    )

    val state: StateFlow<WooPosTotalsViewState> = uiState

    private var dataState: MutableStateFlow<TotalsDataState> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = TotalsDataState(),
        key = KEY_STATE,
    )

    private var debounceJob: Job? = null

    init {
        listenUpEvents()
    }

    fun onUIEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            is WooPosTotalsUIEvent.CollectPaymentClicked -> {
                debounce {
                    viewModelScope.launch {
                        collectPayment()
                    }
                }
            }
            is WooPosTotalsUIEvent.OnNewTransactionClicked -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(
                        ChildToParentEvent.NewTransactionClicked
                    )
                    uiState.value = InitialState
                }
            }
            is WooPosTotalsUIEvent.RetryOrderCreationClicked -> {
                createOrderDraft(dataState.value.productIds)
            }
        }
    }

    private suspend fun collectPayment() {
        val orderId = dataState.value.orderId
        check(orderId != EMPTY_ORDER_ID)
        val result = cardReaderFacade.collectPayment(orderId)
        when (result) {
            is WooPosCardReaderPaymentResult.Success -> {
                val state = uiState.value
                check(state is WooPosTotalsViewState.Totals)
                uiState.value = WooPosTotalsViewState.PaymentSuccess(
                    state.orderSubtotalText,
                    state.orderTaxText,
                    state.orderTotalText
                )
                childrenToParentEventSender.sendToParent(ChildToParentEvent.OrderSuccessfullyPaid)
            }
            else -> Unit
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.CheckoutClicked -> {
                        dataState.value = dataState.value.copy(productIds = event.productIds)
                        createOrderDraft(dataState.value.productIds)
                    }

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        uiState.value = InitialState
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun createOrderDraft(productIds: List<Long>) {
        viewModelScope.launch {
            uiState.value = WooPosTotalsViewState.Loading

            totalsRepository.createOrderWithProducts(productIds = productIds)
                .fold(
                    onSuccess = { order ->
                        dataState.value = dataState.value.copy(orderId = order.id)
                        uiState.value = buildWooPosTotalsViewState(order)
                    },
                    onFailure = { error ->
                        WooLog.e(T.POS, "Order creation failed - $error")
                        uiState.value = WooPosTotalsViewState.Error(
                            resourceProvider.getString(R.string.woopos_totals_order_creation_error)
                        )
                    }
                )
        }
    }

    private suspend fun buildWooPosTotalsViewState(order: Order): WooPosTotalsViewState.Totals {
        val subtotalAmount = order.items.sumOf { it.subtotal }
        val taxAmount = order.totalTax
        val totalAmount = subtotalAmount + taxAmount

        return WooPosTotalsViewState.Totals(
            orderSubtotalText = priceFormat(subtotalAmount),
            orderTaxText = priceFormat(taxAmount),
            orderTotalText = priceFormat(totalAmount),
        )
    }

    @Parcelize
    private data class TotalsDataState(
        val orderId: Long = EMPTY_ORDER_ID,
        val productIds: List<Long> = emptyList()
    ) : Parcelable

    private fun debounce(destinationFunction: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_TIME_MS)
            destinationFunction()
        }
    }
}
