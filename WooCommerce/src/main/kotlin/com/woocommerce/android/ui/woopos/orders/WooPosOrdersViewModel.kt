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

    init { loadOrders() }

    fun onOrderSelected(orderId: Long) {
        selectedId = orderId
        (state.value as? WooPosOrdersState.Content)?.let { s ->
            _state.value = s.copy(
                items = orders.toOrderItems(selectedId),
                selectedOrderId = selectedId
            )
        }
    }

    fun refresh() {
        _state.value = when (val s = _state.value) {
            is WooPosOrdersState.Content -> s.copy(pullToRefreshState = WooPosPullToRefreshState.Refreshing)
            else -> WooPosOrdersState.Loading
        }
        ordersDataSource.clearCache()
        loadOrders()
    }

    fun isRefreshing(): Boolean =
        (state.value as? WooPosOrdersState.Content)
            ?.pullToRefreshState == WooPosPullToRefreshState.Refreshing

    private fun loadOrders() {
        viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error ->
                        handleError(result.message)

                    is LoadOrdersResult.SuccessCache ->
                        handleSuccess(result.orders, isCache = true)

                    is LoadOrdersResult.SuccessRemote ->
                        handleSuccess(result.orders, isCache = false)
                }
            }
        }
    }

    private fun handleError(message: String) {
        _state.value = WooPosOrdersState.Error(message)
    }

    private fun handleSuccess(newOrders: List<Order>, isCache: Boolean) {
        applySelection(newOrders)

        if (newOrders.isEmpty()) {
            handleEmpty(isCache)
        } else {
            setContent(newOrders)
        }
    }

    private fun handleEmpty(isCache: Boolean) {
        if (isCache && isRefreshing() && _state.value is WooPosOrdersState.Content) {
            return
        }
        _state.value = if (isCache) WooPosOrdersState.Loading else WooPosOrdersState.Empty
    }

    private fun setContent(newOrders: List<Order>) {
        _state.value = WooPosOrdersState.Content(
            items = newOrders.toOrderItems(selectedId),
            selectedOrderId = selectedId,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = WooPosPaginationState.None
        )
    }

    private fun applySelection(newOrders: List<Order>) {
        orders = newOrders
        selectedId = chooseSelectedId(selectedId, newOrders)
    }

    private fun chooseSelectedId(current: Long?, newOrders: List<Order>): Long? {
        return current?.takeIf { id -> newOrders.any { it.id == id } }
            ?: newOrders.firstOrNull()?.id
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
