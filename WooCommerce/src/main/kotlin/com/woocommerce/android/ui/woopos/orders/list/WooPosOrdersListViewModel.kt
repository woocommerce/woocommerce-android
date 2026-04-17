package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource.Monotonic

@HiltViewModel
class WooPosOrdersListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderItemMapper: WooPosOrderItemMapper,
) : ViewModel() {

    private val singleOrderId: Long? = savedStateHandle.get<Long>(ORDERS_ROUTE_ORDER_ID_KEY)

    private val _state = MutableStateFlow<WooPosOrdersListState>(
        WooPosOrdersListState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
    )
    val state: StateFlow<WooPosOrdersListState> = _state.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId: StateFlow<Long?> = _selectedOrderId.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var loadingJob: Job? = null

    init {
        if (singleOrderId == null) {
            loadOrders()
        }
    }

    private fun loadOrders(isRefreshing: Boolean = false) {
        cancelJobs()
        val mark = Monotonic.markNow()
        loadingJob = viewModelScope.launch {
            ordersDataSource.loadOrders(forceRefreshRefunds = isRefreshing).collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        _state.value = WooPosOrdersListState.Error(
                            message = result.message,
                            searchInputState = WooPosSearchInputState.Closed
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosOrdersListState.Loading(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.ordersWithRefunds.keys.toList())
                        }
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosOrdersListState.Empty(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.ordersWithRefunds.keys.toList())
                        }
                    }
                }
            }
        }
    }

    private fun replaceOrders(
        orders: List<com.woocommerce.android.model.Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val currentState = _state.value
        val currentFirstOrderId = (currentState as? WooPosOrdersListState.Content)
            ?.let { it.items as? WooPosOrdersListState.Content.Items.Loaded }
            ?.items?.firstOrNull()?.id

        val newFirstOrderId = orders.firstOrNull()?.id

        val currentSelectedId = _selectedOrderId.value
        val isSelectedOrderStillInList = currentSelectedId != null && orders.any { it.id == currentSelectedId }
        val newSelectedId = if (isSelectedOrderStillInList) {
            currentSelectedId
        } else {
            requireNotNull(newFirstOrderId) { "Content requires at least one order" }
        }

        val items = orders.map { order ->
            orderItemMapper.mapOrderItem(order, newSelectedId)
        }

        _state.value = WooPosOrdersListState.Content(
            items = WooPosOrdersListState.Content.Items.Loaded(items),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState
        )

        _selectedOrderId.value = newSelectedId

        viewModelScope.launch {
            if (currentFirstOrderId != null && currentFirstOrderId != newFirstOrderId) {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }

    private fun appendOrders(
        orders: List<com.woocommerce.android.model.Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as? WooPosOrdersListState.Content ?: return
        val loadedItems = current.items as? WooPosOrdersListState.Content.Items.Loaded ?: return

        val newItems = orders.map { order ->
            orderItemMapper.mapOrderItem(order, _selectedOrderId.value)
        }
        val allItems = loadedItems.items + newItems

        _state.value = WooPosOrdersListState.Content(
            items = WooPosOrdersListState.Content.Items.Loaded(allItems),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState
        )
    }

    private fun cancelJobs() {
        loadingJob?.cancel()
    }
}
