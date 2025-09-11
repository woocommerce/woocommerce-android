package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.formatToDDMMMYYYY
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private var loadMoreOrdersJob: Job? = null
    private var loadOrdersJob: Job? = null
    private var loadMoreAfterLoadCompletes = false

    init {
        loadOrders()
    }

    fun onOrderSelected(orderId: Long) {
        val currentState = _state.value
        if (currentState is WooPosOrdersState.Content) {
            _state.value = currentState.copy(
                items = currentState.items.map { it.copy(isSelected = it.id == orderId) },
                selectedOrderId = orderId
            )
        }
    }

    fun onRefresh() {
        val currentState = _state.value
        _state.value = when (currentState) {
            is WooPosOrdersState.Content -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosOrdersState.Empty -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosOrdersState.Error -> currentState
            is WooPosOrdersState.Loading -> currentState
        }

        ordersDataSource.clearCache()
        loadOrders()
    }

    @Suppress("ReturnCount")
    fun onEndOfOrdersListReached() {
        val currentState = _state.value
        if (currentState !is WooPosOrdersState.Content) {
            return
        }

        if (loadOrdersJob?.isActive == true) {
            loadMoreAfterLoadCompletes = true
            _state.value = currentState.copy(paginationState = WooPosPaginationState.Loading)
            return
        }

        if (loadMoreOrdersJob?.isActive == true) {
            return
        }

        if (!ordersDataSource.hasMorePages) {
            return
        }

        _state.value = currentState.copy(paginationState = WooPosPaginationState.Loading)

        loadMoreOrdersJob?.cancel()
        loadMoreOrdersJob = viewModelScope.launch {
            val result = ordersDataSource.loadMore()
            if (result.isSuccess) {
                updateContentState(result.getOrThrow())
            } else {
                _state.value = currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun loadOrders() {
        loadOrdersJob?.cancel()
        loadMoreOrdersJob?.cancel()
        loadOrdersJob = viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        _state.value = WooPosOrdersState.Error(message = result.message)
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        updateContentState(result.orders)
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        if (result.orders.isEmpty()) {
                            _state.value = WooPosOrdersState.Empty()
                        } else {
                            updateContentState(result.orders)
                        }

                        if (loadMoreAfterLoadCompletes) {
                            queueLoadMoreAfterLoadCompletes()
                        }
                    }
                }
            }
        }
    }

    private fun queueLoadMoreAfterLoadCompletes() {
        loadOrdersJob?.invokeOnCompletion { throwable ->
            if (throwable == null && _state.value is WooPosOrdersState.Content) {
                loadMoreAfterLoadCompletes = false
                onEndOfOrdersListReached()
            }
        }
    }

    private fun updateContentState(orders: List<Order>) {
        val currentSelectedId = (_state.value as? WooPosOrdersState.Content)?.selectedOrderId
        val newSelectedId = currentSelectedId?.takeIf { id -> orders.any { it.id == id } }
            ?: orders.firstOrNull()?.id

        _state.value = WooPosOrdersState.Content(
            items = orders.map { order ->
                OrderItemViewState(
                    id = order.id,
                    title = "Order #${order.number}",
                    date = order.dateCreated.formatToDDMMMYYYY(),
                    total = "${order.total} ${order.currency}",
                    isSelected = order.id == newSelectedId
                )
            },
            selectedOrderId = newSelectedId,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = WooPosPaginationState.None
        )
    }
}
