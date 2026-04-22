package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.SearchOrdersResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersCoordinator
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val coordinator: WooPosOrdersCoordinator,
) : ViewModel() {

    private val singleOrderId: Long? = savedStateHandle.get<Long>(ORDERS_ROUTE_ORDER_ID_KEY)

    private val _state = MutableStateFlow<WooPosOrdersListState>(
        WooPosOrdersListState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
    )
    val state: StateFlow<WooPosOrdersListState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var loadingJob: Job? = null
    private var loadingMoreOrdersJob: Job? = null

    private val currentSearchQuery: String?
        get() = (
            (
                _state.value.searchInputState as? WooPosSearchInputState.Open
                )?.input as? WooPosSearchInputState.Open.Input.Query
            )?.query

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
    }

    init {
        if (singleOrderId == null) {
            loadOrders()
        }
        observeOrderRefreshed()
    }

    private fun observeOrderRefreshed() {
        viewModelScope.launch {
            coordinator.orderRefreshed.collect { orderId ->
                refreshOrderItem(orderId)
            }
        }
    }

    fun onOrderSelected(orderId: Long) {
        val current = _state.value as? WooPosOrdersListState.Content ?: return
        val loadedItems = current.items as? WooPosOrdersListState.Content.Items.Loaded ?: return

        if (coordinator.selectedOrderId.value == orderId) return

        val position = loadedItems.items.indexOfFirst { it.id == orderId }.coerceAtLeast(0)
        val selectedItem = loadedItems.items.firstOrNull { it.id == orderId } ?: return

        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrdersListRowTapped(
                orderId = selectedItem.id,
                orderStatus = selectedItem.statusSlug,
                listPosition = position,
                createdAtMillis = selectedItem.createdAtMillis
            )
        }

        val updatedItems = loadedItems.items.map { it.copy(isSelected = it.id == orderId) }
        _state.value = current.copy(
            items = WooPosOrdersListState.Content.Items.Loaded(updatedItems)
        )
        coordinator.selectOrder(orderId)
    }

    fun onRefresh() {
        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrdersListPullToRefreshTriggered()
        }

        val currentState = _state.value
        _state.value = when (currentState) {
            is WooPosOrdersListState.Content -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )
            is WooPosOrdersListState.Empty -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )
            is WooPosOrdersListState.Error -> currentState
            is WooPosOrdersListState.Loading -> currentState
        }

        ordersDataSource.clearCache()

        val query = currentSearchQuery
        if (query.isNullOrEmpty()) {
            loadOrders()
        } else {
            performSearch(query, isRefreshing = true)
        }
    }

    fun onEndOfOrdersListReached() {
        val currentState = _state.value
        if (currentState !is WooPosOrdersListState.Content ||
            currentState.paginationState != WooPosPaginationState.None ||
            currentState.pullToRefreshState == WooPosPullToRefreshState.Refreshing
        ) {
            return
        }

        loadMoreIfPossible()
    }

    fun onPaginationErrorTryAgain() {
        loadMoreIfPossible()
    }

    fun onSearchEvent(event: WooPosSearchUIEvent) {
        when (event) {
            is WooPosSearchUIEvent.SearchIconClicked -> {
                viewModelScope.launch {
                    ordersAnalyticsTracker.trackOrdersListSearchButtonTapped()
                }
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Hint(
                            resourceProvider.getString(R.string.woopos_search_orders)
                        ),
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
                if (event.query.isEmpty()) {
                    loadOrders()
                } else {
                    performSearch(event.query)
                }
            }

            is WooPosSearchUIEvent.Clear -> {
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Hint(
                            resourceProvider.getString(R.string.woopos_search_orders)
                        ),
                        isLoading = false,
                        requestFocus = true
                    )
                )
                loadOrders()
            }

            is WooPosSearchUIEvent.Close -> {
                updateSearchState(WooPosSearchInputState.Closed)
                loadOrders()
            }
        }
    }

    fun onSearchErrorRetry() {
        val query = currentSearchQuery
        if (!query.isNullOrEmpty()) {
            performSearch(query)
        }
    }

    fun onOrdersEmptyActionClicked() {
        onRefresh()
    }

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosOrdersListState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
        loadOrders()
    }

    private fun refreshOrderItem(orderId: Long) {
        viewModelScope.launch {
            val order = ordersDataSource.getOrderById(orderId).getOrNull() ?: return@launch
            val current = _state.value as? WooPosOrdersListState.Content ?: return@launch
            val loadedItems = current.items as? WooPosOrdersListState.Content.Items.Loaded ?: return@launch

            val updatedItems = loadedItems.items.map { item ->
                if (item.id == orderId) {
                    orderItemMapper.mapOrderItem(order, coordinator.selectedOrderId.value)
                } else {
                    item
                }
            }
            _state.value = current.copy(
                items = WooPosOrdersListState.Content.Items.Loaded(updatedItems)
            )
        }
    }

    private fun loadOrders() {
        cancelJobs()
        val mark = Monotonic.markNow()
        loadingJob = viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        coordinator.selectOrder(null)
                        _state.value = WooPosOrdersListState.Error(
                            message = result.message,
                            searchInputState = WooPosSearchInputState.Closed
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        if (result.orders.isEmpty()) {
                            _state.value = WooPosOrdersListState.Loading(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.orders)
                        }
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        if (result.orders.isEmpty()) {
                            _state.value = WooPosOrdersListState.Empty(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.orders)
                        }
                    }
                }
            }
        }
    }

    private fun performSearch(query: String, isRefreshing: Boolean = false) {
        cancelJobs()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            if (!isRefreshing) {
                _state.value = WooPosOrdersListState.Content(
                    items = WooPosOrdersListState.Content.Items.Searching,
                    pullToRefreshState = WooPosPullToRefreshState.Disabled,
                    searchInputState = _state.value.searchInputState,
                    paginationState = WooPosPaginationState.None
                )
            }

            val mark = Monotonic.markNow()
            val result = ordersDataSource.searchOrders(query)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds
            ordersAnalyticsTracker.trackOrdersListSearchResultsFetched(elapsedMs)
            when (result) {
                is SearchOrdersResult.Error -> {
                    _state.value = WooPosOrdersListState.Content(
                        items = WooPosOrdersListState.Content.Items.Error(
                            title = resourceProvider.getString(R.string.woopos_search_orders_error_title),
                            message = resourceProvider.getString(R.string.woopos_search_orders_error_description)
                        ),
                        pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        searchInputState = _state.value.searchInputState,
                        paginationState = WooPosPaginationState.None
                    )
                }

                is SearchOrdersResult.Success -> {
                    if (result.orders.isEmpty()) {
                        coordinator.selectOrder(null)
                        _state.value = WooPosOrdersListState.Content(
                            items = WooPosOrdersListState.Content.Items.NothingFound(
                                title = resourceProvider.getString(R.string.woopos_search_orders_empty_title),
                                message = resourceProvider.getString(R.string.woopos_search_orders_empty_description)
                            ),
                            pullToRefreshState = WooPosPullToRefreshState.Enabled,
                            searchInputState = _state.value.searchInputState,
                            paginationState = WooPosPaginationState.None
                        )
                    } else {
                        replaceOrders(result.orders)
                    }
                }
            }
        }
    }

    private fun loadMoreIfPossible() {
        if (loadingJob?.isActive == true || loadingMoreOrdersJob?.isActive == true) return
        if (!ordersDataSource.hasMorePages) return

        val currentState = _state.value
        val newState = when (currentState) {
            is WooPosOrdersListState.Content -> currentState.copy(
                paginationState = WooPosPaginationState.Loading
            )
            else -> return
        }
        _state.value = newState

        loadingMoreOrdersJob?.cancel()
        loadingMoreOrdersJob = viewModelScope.launch {
            val normalizedQuery = currentSearchQuery.takeUnless { it.isNullOrEmpty() }
            val result = ordersDataSource.loadMore(normalizedQuery)

            if (result.isSuccess) {
                ordersAnalyticsTracker.trackOrdersListNextPageLoaded()
                appendOrders(result.getOrThrow())
            } else {
                _state.value = newState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }

    private fun replaceOrders(
        orders: List<Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val currentState = _state.value
        val currentFirstOrderId = (currentState as? WooPosOrdersListState.Content)
            ?.let { it.items as? WooPosOrdersListState.Content.Items.Loaded }
            ?.items?.firstOrNull()?.id

        val newFirstOrderId = orders.firstOrNull()?.id

        val currentSelectedId = coordinator.selectedOrderId.value
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

        coordinator.selectOrder(newSelectedId)

        viewModelScope.launch {
            if (currentFirstOrderId != null && currentFirstOrderId != newFirstOrderId) {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }

    private fun appendOrders(
        orders: List<Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as? WooPosOrdersListState.Content ?: return
        val loadedItems = current.items as? WooPosOrdersListState.Content.Items.Loaded ?: return

        val newItems = orders.map { order ->
            orderItemMapper.mapOrderItem(order, coordinator.selectedOrderId.value)
        }
        val allItems = loadedItems.items + newItems

        _state.value = WooPosOrdersListState.Content(
            items = WooPosOrdersListState.Content.Items.Loaded(allItems),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState
        )
    }

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        _state.value = when (val currentState = _state.value) {
            is WooPosOrdersListState.Content -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersListState.Empty -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersListState.Error -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersListState.Loading -> currentState.copy(searchInputState = searchState)
        }
    }

    private fun cancelJobs() {
        searchJob?.cancel()
        loadingJob?.cancel()
        loadingMoreOrdersJob?.cancel()
    }
}
