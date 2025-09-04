package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.formatToDDMMMYYYY
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(WooPosOrdersState.Loading)
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    // Keep the domain list & selection internally
    private var orders: List<Order> = emptyList()
    private var selectedId: Long? = null

    init {
        refreshOrders()
    }

    fun onOrderSelected(orderId: Long) {
        selectedId = orderId
        val s = _state.value
        if (s is WooPosOrdersState.Content) {
            _state.value = s.copy(
                items = orders.toOrderItems(selectedId),
                selectedOrderId = selectedId
            )
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _state.value = WooPosOrdersState.Loading

            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        // If we already had content (e.g., cache emitted first) keep it and surface an error?
                        // For now, match current behavior: show an error state.
                        _state.value = WooPosOrdersState.Error(
                            message = result.message
                        )
                    }

                    is LoadOrdersResult.Success -> {
                        orders = result.orders

                        // Preserve previous selection if still present; otherwise pick first
                        selectedId = selectedId?.takeIf { id -> orders.any { it.id == id } }
                            ?: orders.firstOrNull()?.id

                        if (orders.isEmpty()) {
                            _state.value = WooPosOrdersState.Empty
                        } else {
                            _state.value = WooPosOrdersState.Content(
                                items = orders.toOrderItems(selectedId),
                                selectedOrderId = selectedId,
                                // PTR/pagination are for later; set sensible defaults
                                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                                paginationState = WooPosPaginationState.None,
                                listError = null
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Map domain orders to lightweight UI rows, deriving selection from a single source of truth. */
private fun List<Order>.toOrderItems(selectedId: Long?): List<OrderItemViewState> =
    map { o ->
        OrderItemViewState(
            id = o.id,
            title = "Order #${o.number}",
            date = o.dateCreated.formatToDDMMMYYYY(),
            total = "${o.total} ${o.currency}",
            isSelected = o.id == selectedId
        )
    }
