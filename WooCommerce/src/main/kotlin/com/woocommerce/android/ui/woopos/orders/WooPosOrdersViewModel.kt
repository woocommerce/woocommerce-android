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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(WooPosOrdersState.Loading)
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    private var orders: List<Order> = emptyList()
    private var selectedId: Long? = null

    init {
        loadOrders(loadFromCacheFirst = true)
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

    fun refresh() {
        val s = _state.value
        _state.value = when (s) {
            is WooPosOrdersState.Content -> s.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            else -> WooPosOrdersState.Loading
        }
        loadOrders(loadFromCacheFirst = false)
    }

    private fun loadOrders(loadFromCacheFirst: Boolean) {
        viewModelScope.launch {
            ordersDataSource.loadOrders(loadFromCacheFirst).collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        _state.value = WooPosOrdersState.Error(message = result.message)
                    }
                    is LoadOrdersResult.Success -> {
                        val newOrders = result.orders
                        orders = newOrders
                        selectedId = selectedId?.takeIf { id -> newOrders.any { it.id == id } }
                            ?: newOrders.firstOrNull()?.id

                        if (newOrders.isEmpty()) {
                            if (loadFromCacheFirst) {
                                _state.value = WooPosOrdersState.Loading
                            } else {
                                _state.value = WooPosOrdersState.Empty
                            }
                        } else {
                            _state.value = WooPosOrdersState.Content(
                                items = newOrders.toOrderItems(selectedId),
                                selectedOrderId = selectedId,
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
