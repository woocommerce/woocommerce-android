package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.formatToDDMMMYYYY
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(
        WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
    )
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    private var searchJob: Job? = null

    private val currentSearchQuery: String?
        get() = ((_state.value.searchInputState as? WooPosSearchInputState.Open)?.input
            as? WooPosSearchInputState.Open.Input.Query)?.query

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
    }

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

        val query = currentSearchQuery
        if (query.isNullOrEmpty()) {
            loadOrders()
        } else {
            performSearch(query)
        }
    }

    fun onSearchEvent(event: WooPosSearchUIEvent) {
        when (event) {
            is WooPosSearchUIEvent.SearchIconClicked -> {
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query("", 0),
                        isLoading = false,
                        requestFocus = true
                    )
                )
            }

            is WooPosSearchUIEvent.Search -> {
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query(
                            event.query,
                            event.cursorPosition
                        ),
                        isLoading = false,
                    )
                )
                performSearch(event.query)
            }

            is WooPosSearchUIEvent.Clear -> {
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query("", 0),
                        isLoading = false,
                        requestFocus = true
                    )
                )
                performSearch("")
            }

            is WooPosSearchUIEvent.Close -> {
                updateSearchState(WooPosSearchInputState.Closed)
                loadOrders()
            }
        }
    }

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        _state.value = when (val currentState = _state.value) {
            is WooPosOrdersState.Content -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Empty -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Error -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Loading -> currentState.copy(searchInputState = searchState)
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = WooPosOrdersState.Loading(searchInputState = _state.value.searchInputState)
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            val result = ordersDataSource.searchOrders(query)
            when (result) {
                is SearchOrdersResult.Error -> {
                    _state.value = WooPosOrdersState.Error(
                        message = result.message,
                        searchInputState = _state.value.searchInputState
                    )
                }

                is SearchOrdersResult.Success -> {
                    if (result.orders.isEmpty()) {
                        _state.value = WooPosOrdersState.Empty(
                            searchInputState = _state.value.searchInputState
                        )
                    } else {
                        updateContentState(result.orders)
                    }
                }
            }
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _state.value = WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        _state.value = WooPosOrdersState.Error(
                            message = result.message,
                            searchInputState = WooPosSearchInputState.Closed
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        updateContentState(result.orders)
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        if (result.orders.isEmpty()) {
                            _state.value = WooPosOrdersState.Empty(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            updateContentState(result.orders)
                        }
                    }
                }
            }
        }
    }

    private fun updateContentState(orders: List<Order>) {
        val currentState = _state.value
        val currentSelectedId = (currentState as? WooPosOrdersState.Content)?.selectedOrderId
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
            paginationState = WooPosPaginationState.None,
            searchInputState = currentState.searchInputState,
        )
    }
}
