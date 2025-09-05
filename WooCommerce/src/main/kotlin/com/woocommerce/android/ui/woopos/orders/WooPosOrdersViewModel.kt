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
        loadOrders()
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
        ordersDataSource.clearCache()
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        _state.value = WooPosOrdersState.Error(message = result.message)
                    }

                    is LoadOrdersResult.SuccessCache,
                    is LoadOrdersResult.SuccessRemote -> {
                        val newOrders = when (result) {
                            is LoadOrdersResult.SuccessCache -> result.orders
                            is LoadOrdersResult.SuccessRemote -> result.orders
                            else -> emptyList()
                        }

                        orders = newOrders
                        selectedId = chooseSelectedId(selectedId, newOrders)

                        _state.value = if (newOrders.isEmpty()) {
                            toStateForEmpty(result)
                        } else {
                            WooPosOrdersState.Content(
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

    private fun chooseSelectedId(
        current: Long?,
        newOrders: List<Order>
    ): Long? {
        return current?.takeIf { id -> newOrders.any { it.id == id } }
            ?: newOrders.firstOrNull()?.id
    }

    private fun toStateForEmpty(result: LoadOrdersResult): WooPosOrdersState = when (result) {
        is LoadOrdersResult.SuccessCache -> WooPosOrdersState.Loading
        is LoadOrdersResult.SuccessRemote -> WooPosOrdersState.Empty
        is LoadOrdersResult.Error -> WooPosOrdersState.Error(result.message)
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
