package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.math.RoundingMode

@HiltViewModel(assistedFactory = WooPosRefundViewModel.Factory::class)
class WooPosRefundViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val getRefundableItems: WooPosGetRefundableItems,
    private val groupRefundItems: WooPosGroupRefundItems,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(orderId: Long): WooPosRefundViewModel
    }

    private val _state = MutableStateFlow<WooPosRefundState>(WooPosRefundState.Loading)
    val state: StateFlow<WooPosRefundState> = _state.asStateFlow()

    private var currentOrder: Order? = null

    init {
        loadRefundableItems()
    }

    private fun loadRefundableItems() {
        viewModelScope.launch {
            _state.value = WooPosRefundState.Loading

            val orderResult = ordersDataSource.refreshOrderById(orderId)
            if (orderResult.isFailure) {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic)
                )
                return@launch
            }

            val order = orderResult.getOrThrow()
            currentOrder = order

            val refundsResult = retrieveOrderRefunds(order)
            val refunds = if (refundsResult.isSuccess) {
                refundsResult.getOrThrow()
            } else {
                emptyList()
            }

            val refundableItems = getRefundableItems(order, refunds)

            if (refundableItems.isEmpty()) {
                _state.value = WooPosRefundState.NoRefundableItems
                return@launch
            }

            _state.value = buildContentState(
                order = order,
                refundableItems = refundableItems
            )
        }
    }

    private fun buildContentState(
        order: Order,
        refundableItems: List<WooPosRefundableItem>
    ): WooPosRefundState.Content {
        val itemsByOrderItemId = refundableItems.groupBy { it.orderItemId }

        val subtotal = itemsByOrderItemId.entries.sumOf { (_, items) ->
            val quantity = items.size.toBigDecimal()
            quantity.multiply(items.first().unitPrice)
        }

        val taxes: BigDecimal = itemsByOrderItemId.entries.sumOf { (orderItemId, items) ->
            val originalItem = order.items.find { it.itemId == orderItemId }
            val refundQuantity = items.size

            if (originalItem != null && originalItem.quantity > 0) {
                if (refundQuantity.toFloat() == originalItem.quantity) {
                    originalItem.totalTax
                } else {
                    val singleItemTax = originalItem.totalTax.divide(
                        originalItem.quantity.toBigDecimal(),
                        2,
                        RoundingMode.HALF_UP
                    )
                    refundQuantity.toBigDecimal().multiply(singleItemTax)
                }
            } else {
                BigDecimal.ZERO
            }
        }

        val total = subtotal + taxes

        return WooPosRefundState.Content(
            orderId = order.id,
            orderNumber = "#${order.number}",
            currency = order.currency,
            refundableItems = refundableItems,
            itemsCount = refundableItems.size,
            subtotal = subtotal,
            taxes = taxes,
            total = total,
            formattedSubtotal = PriceUtils.formatCurrency(subtotal, order.currency, currencyFormatter),
            formattedTaxes = PriceUtils.formatCurrency(taxes, order.currency, currencyFormatter),
            formattedTotal = PriceUtils.formatCurrency(total, order.currency, currencyFormatter),
            paymentMethod = "TEST: payment card ••••1456", // TBD: use real payment method value
            step = WooPosRefundState.Content.RefundStep.SelectItems
        )
    }

    fun onUIEvent(event: WooPosRefundUIEvent) {
        when (event) {
            WooPosRefundUIEvent.DialogDismissed -> {
                when (val currentState = _state.value) {
                    is WooPosRefundState.RefundSuccess,
                    is WooPosRefundState.RefundError,
                    is WooPosRefundState.Error,
                    is WooPosRefundState.Loading,
                    is WooPosRefundState.NoRefundableItems, -> loadRefundableItems()
                    is WooPosRefundState.Content -> {
                        loadRefundableItems()
                        _state.value = currentState.copy(
                            step = WooPosRefundState.Content.RefundStep.SelectItems
                        )
                    }
                }
                loadRefundableItems()
            }
            else -> {
                val currentState = _state.value as? WooPosRefundState.Content ?: return

                when (event) {
                    WooPosRefundUIEvent.ContinueToReviewClicked ->
                        _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ReviewRefund)
                    WooPosRefundUIEvent.BackToSelectItemsClicked ->
                        _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.SelectItems)
                    WooPosRefundUIEvent.ContinueToConfirmRefundClicked ->
                        _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ConfirmRefund)
                    WooPosRefundUIEvent.BackToReviewClicked ->
                        _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ReviewRefund)
                    WooPosRefundUIEvent.OnRefundConfirmed -> processRefund(currentState)
                    WooPosRefundUIEvent.DialogDismissed -> Unit
                }
            }
        }
    }

    private fun processRefund(contentState: WooPosRefundState.Content) {
        viewModelScope.launch {
            _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.Processing)

            val order = currentOrder ?: run {
                _state.value = WooPosRefundState.RefundError(
                    message = resourceProvider.getString(R.string.error_generic)
                )
                return@launch
            }

            val refundItems = groupRefundItems(contentState.refundableItems, order)

            val result = refundStore.createItemsRefund(
                site = selectedSite.get(),
                orderId = contentState.orderId,
                amount = contentState.total,
                reason = "",
                restockItems = true,
                autoRefund = false,
                items = refundItems
            )

            if (result.isError) {
                _state.value = WooPosRefundState.RefundError(
                    message = result.error.message ?: resourceProvider.getString(R.string.error_generic)
                )
            } else {
                _state.value = WooPosRefundState.RefundSuccess(
                    orderId = contentState.orderId,
                    orderNumber = contentState.orderNumber,
                    refundedAmount = contentState.formattedTotal
                )
            }
        }
    }
}
