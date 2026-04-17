package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.SearchOrdersResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
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
    private val orderDetailsMapper: WooPosOrderDetailsMapper,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
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
    }

    fun onOrderSelected(orderId: Long) {
        val current = _state.value as? WooPosOrdersState.Content ?: return
        val loadedItems = current.items as? WooPosOrdersState.Content.Items.Loaded ?: return

        refreshOrderJob?.cancel()

        if (isOrderFullyLoaded(current, orderId)) {
            cacheRefundDataFromLazyState(orderId, loadedItems)
            return
        }

        trackOrderSelectionAnalytics(loadedItems, orderId)

        viewModelScope.launch {
            val orderDetailsViewState = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            val details = computeOrderDetails(orderId, orderDetailsViewState)
            val updatedItems = updateItemsSelection(loadedItems, orderId, details)

            _state.value = current.copy(
                items = WooPosOrdersState.Content.Items.Loaded(items = updatedItems),
                selectedDetails = details
            )

            sideLoadOrderExtras(orderId, details, orderDetailsViewState, loadedItems)
        }
    }

    private fun cacheRefundDataFromLazyState(
        orderId: Long,
        loadedItems: WooPosOrdersState.Content.Items.Loaded
    ) {
        val orderDetailsViewState = loadedItems.items.values.firstOrNull { it.orderId == orderId }
        if (orderDetailsViewState is WooPosOrdersState.OrderDetailsViewState.Lazy) {
            val order = orderDetailsViewState.order
            val refundInfo = refundInfoBuilder.buildRefundInfo(order, orderDetailsViewState.refundResult)
            cacheRefundData(orderId, refundInfo.refundRows, order)
        }
    }

    private fun isOrderFullyLoaded(current: WooPosOrdersState.Content, orderId: Long): Boolean {
        return current.selectedDetails?.id == orderId &&
            current.selectedDetails.actionsState is WooPosOrdersState.OrderActionsState.Loaded
    }

    private fun trackOrderSelectionAnalytics(
        loadedItems: WooPosOrdersState.Content.Items.Loaded,
        orderId: Long
    ) {
        val keys = loadedItems.items.keys.toList()
        val position = keys.indexOfFirst { it.id == orderId }.coerceAtLeast(0)
        val selectedItem = keys.firstOrNull { it.id == orderId } ?: return

        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrdersListRowTapped(
                orderId = selectedItem.id,
                orderStatus = selectedItem.statusSlug,
                listPosition = position,
                createdAtMillis = selectedItem.createdAtMillis
            )
            ordersAnalyticsTracker.trackOrderDetailsLoaded(
                orderId = selectedItem.id,
                orderStatus = selectedItem.statusSlug,
                createdAtMillis = selectedItem.createdAtMillis
            )
        }
    }

    private suspend fun computeOrderDetails(
        orderId: Long,
        orderDetailsViewState: WooPosOrdersState.OrderDetailsViewState?
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details {
        return if (orderDetailsViewState is WooPosOrdersState.OrderDetailsViewState.Lazy) {
            getOrComputeDetailsWithoutActions(orderId)
        } else {
            getOrComputeDetails(orderId)
        }
    }

    private fun updateItemsSelection(
        loadedItems: WooPosOrdersState.Content.Items.Loaded,
        orderId: Long,
        details: WooPosOrdersState.OrderDetailsViewState.Computed.Details
    ): Map<WooPosOrdersState.OrderItemViewState, WooPosOrdersState.OrderDetailsViewState> {
        return loadedItems.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == orderId)
        }.mapValues { (item, orderDetails) ->
            if (item.id == orderId && orderDetails is WooPosOrdersState.OrderDetailsViewState.Lazy) {
                WooPosOrdersState.OrderDetailsViewState.Computed(orderId = orderId, details = details)
            } else {
                orderDetails
            }
        }
    }

    private suspend fun sideLoadOrderExtras(
        orderId: Long,
        details: WooPosOrdersState.OrderDetailsViewState.Computed.Details,
        orderDetailsViewState: WooPosOrdersState.OrderDetailsViewState?,
        loadedItems: WooPosOrdersState.Content.Items.Loaded
    ) {
        sideLoadBookings(orderId, details)

        if (details.actionsState !is WooPosOrdersState.OrderActionsState.Loading) {
            ensureRefundDataCached(orderId, orderDetailsViewState, loadedItems)
            return
        }

        val order = resolveOrder(orderId, orderDetailsViewState, loadedItems) ?: return
        sideLoadActionsAndRefundData(orderId, order)
    }

    private suspend fun ensureRefundDataCached(
        orderId: Long,
        orderDetailsViewState: WooPosOrdersState.OrderDetailsViewState?,
        loadedItems: WooPosOrdersState.Content.Items.Loaded
    ) {
        if (cachedRefundData?.orderId == orderId) return

        val order = resolveOrder(orderId, orderDetailsViewState, loadedItems) ?: return
        val refundsResult = retrieveOrderRefunds(order, forceRefresh = false).fold(
            onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
            onFailure = { RefundsFetchResult.Error }
        )
        val refundInfo = refundInfoBuilder.buildRefundInfo(order, refundsResult)
        cacheRefundData(orderId, refundInfo.refundRows, order)
    }

    private suspend fun resolveOrder(
        orderId: Long,
        orderDetailsViewState: WooPosOrdersState.OrderDetailsViewState?,
        loadedItems: WooPosOrdersState.Content.Items.Loaded
    ): Order? {
        return when (orderDetailsViewState) {
            is WooPosOrdersState.OrderDetailsViewState.Lazy -> orderDetailsViewState.order
            is WooPosOrdersState.OrderDetailsViewState.Computed -> {
                loadedItems.items.keys.firstOrNull { it.id == orderId }?.let { item ->
                    ordersDataSource.getOrderById(item.id).getOrNull()
                }
            }
            else -> null
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrdersListPullToRefreshTriggered()
        }

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
            loadOrders(isRefreshing = true)
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
        viewModelScope.launch {
            _openUrlEvent.emit(AppUrls.URL_LEARN_MORE_ORDERS)
        }
    }

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosOrdersState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
        loadOrders()
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
            val result = ordersDataSource.searchOrders(query, forceRefreshRefunds = isRefreshing)
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
                    if (result.ordersWithRefunds.isEmpty()) {
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
                        replaceOrders(result.ordersWithRefunds.keys.toList())
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
                appendOrders(result.getOrThrow().keys.toList())
            } else {
                _state.value = newState.copy(paginationState = WooPosPaginationState.Error)
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
