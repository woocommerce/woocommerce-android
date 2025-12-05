package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
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

@HiltViewModel(assistedFactory = WooPosRefundViewModel.Factory::class)
class WooPosRefundViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val getRefundableItems: WooPosGetRefundableItems,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(orderId: Long): WooPosRefundViewModel
    }

    private val _state = MutableStateFlow<WooPosRefundState>(WooPosRefundState.Loading)
    val state: StateFlow<WooPosRefundState> = _state.asStateFlow()

    init {
        loadRefundableItems()
    }

    private fun loadRefundableItems() {
        viewModelScope.launch {
            _state.value = WooPosRefundState.Loading

            val orderResult = ordersDataSource.getOrderById(orderId)
            if (orderResult.isFailure) {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic)
                )
                return@launch
            }

            val order = orderResult.getOrThrow()

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
        val subtotal = refundableItems.sumOf { it.lineTotal }
        val taxes = refundableItems.sumOf { it.lineTax }
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
            step = WooPosRefundState.Content.RefundStep.SelectItems
        )
    }

    fun onUIEvent(event: WooPosRefundUIEvent) {
        val currentState = _state.value as? WooPosRefundState.Content ?: return

        val newStep = when (event) {
            WooPosRefundUIEvent.ContinueToReviewClicked ->
                WooPosRefundState.Content.RefundStep.ReviewRefund
            WooPosRefundUIEvent.BackToSelectItemsClicked ->
                WooPosRefundState.Content.RefundStep.SelectItems
            WooPosRefundUIEvent.DialogDismissed ->
                WooPosRefundState.Content.RefundStep.SelectItems
        }

        _state.value = currentState.copy(step = newStep)
    }
}
