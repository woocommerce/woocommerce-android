package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.SearchOrdersResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersUIEvent
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

@Suppress("LargeClass", "MagicNumber")
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderItemMapper: WooPosOrderItemMapper,
    private val orderDetailsMapper: WooPosOrderDetailsMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosOrderActionsProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(
        WooPosBookingsState.Loading
    )
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var loadingJob: Job? = null
    private var loadingMoreOrdersJob: Job? = null
    private var sideLoadActionsJob: Job? = null

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
        loadOrders()
    }

    fun onUIEvent(event: WooPosOrdersUIEvent) {
        when (event) {
            is WooPosOrdersUIEvent.OrderActionClicked -> handleActionClicked(event.action)
        }
    }

    private fun handleActionClicked(action: WooPosBookingsState.OrderAction) {
        when (action) {
            is WooPosBookingsState.OrderAction.EmailReceipt -> onEmailReceiptButtonClicked(action.orderId)
            is WooPosBookingsState.OrderAction.IssueRefund -> onIssueRefundButtonClicked(action.orderId)
        }
    }

    fun onOrderSelected(orderId: Long) {
        val current = _state.value as? WooPosBookingsState.Content ?: return
        val loadedItems = current.items as? WooPosBookingsState.Content.Items.Loaded ?: return

        if (isAlreadySelectedWithActions(current, orderId)) return

        trackOrderSelectionAnalytics(loadedItems, orderId)

        viewModelScope.launch {
            val orderDetailsViewState = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            val details = computeOrderDetails(orderId, orderDetailsViewState)
            val updatedItems = updateItemsSelection(loadedItems, orderId, details)

            _state.value = current.copy(
                items = WooPosBookingsState.Content.Items.Loaded(items = updatedItems),
                selectedDetails = details
            )

            startSideLoadActionsIfNeeded(orderId, details, orderDetailsViewState, loadedItems)
        }
    }

    private fun isAlreadySelectedWithActions(current: WooPosBookingsState.Content, orderId: Long): Boolean {
        return current.selectedDetails?.id == orderId &&
            current.selectedDetails.actionsState is WooPosBookingsState.OrderActionsState.Loaded
    }

    private fun trackOrderSelectionAnalytics(
        loadedItems: WooPosBookingsState.Content.Items.Loaded,
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
        orderDetailsViewState: WooPosBookingsState.OrderDetailsViewState?
    ): WooPosBookingsState.OrderDetailsViewState.Computed.Details {
        return if (orderDetailsViewState is WooPosBookingsState.OrderDetailsViewState.Lazy) {
            getOrComputeDetailsWithoutActions(orderId)
        } else {
            getOrComputeDetails(orderId)
        }
    }

    private fun updateItemsSelection(
        loadedItems: WooPosBookingsState.Content.Items.Loaded,
        orderId: Long,
        details: WooPosBookingsState.OrderDetailsViewState.Computed.Details
    ): Map<WooPosBookingsState.OrderItemViewState, WooPosBookingsState.OrderDetailsViewState> {
        return loadedItems.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == orderId)
        }.mapValues { (item, orderDetails) ->
            if (item.id == orderId && orderDetails is WooPosBookingsState.OrderDetailsViewState.Lazy) {
                WooPosBookingsState.OrderDetailsViewState.Computed(orderId = orderId, details = details)
            } else {
                orderDetails
            }
        }
    }

    private suspend fun startSideLoadActionsIfNeeded(
        orderId: Long,
        details: WooPosBookingsState.OrderDetailsViewState.Computed.Details,
        orderDetailsViewState: WooPosBookingsState.OrderDetailsViewState?,
        loadedItems: WooPosBookingsState.Content.Items.Loaded
    ) {
        if (details.actionsState !is WooPosBookingsState.OrderActionsState.Loading) return

        val order = getOrderForSideLoading(orderId, orderDetailsViewState, loadedItems) ?: return
        sideLoadActions(orderId, order)
    }

    private suspend fun getOrderForSideLoading(
        orderId: Long,
        orderDetailsViewState: WooPosBookingsState.OrderDetailsViewState?,
        loadedItems: WooPosBookingsState.Content.Items.Loaded
    ): Order? {
        return when (orderDetailsViewState) {
            is WooPosBookingsState.OrderDetailsViewState.Lazy -> orderDetailsViewState.order
            is WooPosBookingsState.OrderDetailsViewState.Computed -> {
                loadedItems.items.keys.firstOrNull { it.id == orderId }?.let { item ->
                    ordersDataSource.getOrderById(item.id).getOrNull()
                }
            }

            else -> null
        }
    }

    private fun sideLoadActions(orderId: Long, order: Order) {
        sideLoadActionsJob?.cancel()
        sideLoadActionsJob = viewModelScope.launch {
            val refundsResult = retrieveOrderRefunds(order = order, forceRefresh = true).fold(
                onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
                onFailure = { RefundsFetchResult.Error }
            )

            val stateAfterRefundsFetch = _state.value as? WooPosBookingsState.Content
            if (stateAfterRefundsFetch?.selectedDetails?.id != orderId) {
                return@launch
            }

            val actions = orderActionsProvider.getAvailableActions(order, refundsResult)
            val refundInfo = refundInfoBuilder.buildRefundInfo(order, refundsResult)
            val updatedBreakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)

            val updatedState = _state.value as? WooPosBookingsState.Content ?: return@launch
            val updatedSelectedDetails = updatedState.selectedDetails

            if (updatedSelectedDetails?.id == orderId &&
                updatedSelectedDetails.actionsState is WooPosBookingsState.OrderActionsState.Loading
            ) {
                val loadedItems = updatedState.items as? WooPosBookingsState.Content.Items.Loaded
                val updatedItems = loadedItems?.items?.map { (item, details) ->
                    val updatedDetails =
                        if (item.id == orderId && details is WooPosBookingsState.OrderDetailsViewState.Computed) {
                            WooPosBookingsState.OrderDetailsViewState.Computed(
                                orderId = orderId,
                                details = details.details.copy(
                                    actionsState = WooPosBookingsState.OrderActionsState.Loaded(actions),
                                    breakdown = updatedBreakdown
                                )
                            )
                        } else {
                            details
                        }
                    item to updatedDetails
                }?.toMap()

                _state.value = updatedState.copy(
                    items = if (updatedItems != null) {
                        WooPosBookingsState.Content.Items.Loaded(updatedItems)
                    } else {
                        updatedState.items
                    },
                    selectedDetails = updatedSelectedDetails.copy(
                        actionsState = WooPosBookingsState.OrderActionsState.Loaded(actions),
                        breakdown = updatedBreakdown
                    )
                )
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrdersListPullToRefreshTriggered()
        }

        val currentState = _state.value
        _state.value = when (currentState) {
            is WooPosBookingsState.Content -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosBookingsState.Empty -> currentState.copy(
                pullToRefreshState = WooPosPullToRefreshState.Refreshing
            )

            is WooPosBookingsState.Error -> currentState
            is WooPosBookingsState.Loading -> currentState
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
        if (currentState !is WooPosBookingsState.Content ||
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

    fun onEmailReceiptButtonClicked(orderId: Long) {
        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrderDetailsEmailReceiptTapped()
            childrenToParentEventSender.sendToParent(
                ToEmailReceipt(orderId)
            )
        }
    }

    fun onIssueRefundButtonClicked(orderId: Long) {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.IssueRefund(
                orderId = orderId
            )
        )
    }

    fun onIssueRefundDialogDismissed() {
        val currentState = _state.value as? WooPosBookingsState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosBookingsState.Content.DialogState.Hidden
        )
        refreshSelectedOrder()
    }

    fun onOrdersEmptyActionClicked() {
        viewModelScope.launch {
            _openUrlEvent.emit(AppUrls.URL_LEARN_MORE_ORDERS)
        }
    }

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosBookingsState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
        loadOrders()
    }

    fun onSearchErrorRetry() {
        val query = currentSearchQuery
        if (!query.isNullOrEmpty()) {
            performSearch(query)
        }
    }

    fun loadMoreIfPossible() {
        if (loadingJob?.isActive == true || loadingMoreOrdersJob?.isActive == true) return
        if (!ordersDataSource.hasMorePages) return

        val currentState = _state.value
        val newState = when (currentState) {
            is WooPosBookingsState.Content -> currentState.copy(paginationState = WooPosPaginationState.Loading)
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

    fun onBackFromSuccessfullySendingEmailReceipt() {
        refreshSelectedOrder()
    }

    private fun refreshSelectedOrder() {
        val current = _state.value as? WooPosBookingsState.Content ?: return
        val selectedOrderId = current.selectedDetails?.id ?: return

        sideLoadActionsJob?.cancel()
        viewModelScope.launch {
            ordersDataSource.refreshOrderById(selectedOrderId)
                .onSuccess { applyOrderUpdate(it) }
        }
    }

    private suspend fun applyOrderUpdate(updated: Order) {
        val current = _state.value as? WooPosBookingsState.Content ?: return
        val loaded = current.items as? WooPosBookingsState.Content.Items.Loaded ?: return

        val historicalRefundsResult = retrieveOrderRefunds(updated, forceRefresh = true).fold(
            onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
            onFailure = { RefundsFetchResult.Error }
        )

        val selectedId = loaded.items.keys.firstOrNull { it.isSelected }?.id
        val newItem = orderItemMapper.mapOrderItem(updated, selectedId)
        val newDetailsViewState = orderDetailsMapper.mapOrderDetails(updated, historicalRefundsResult)
        val newDetails = WooPosBookingsState.OrderDetailsViewState.Computed(
            orderId = updated.id,
            details = newDetailsViewState
        )

        val newMap = loaded.items.entries.associate { (item, details) ->
            if (item.id == updated.id) newItem to newDetails else item to details
        }

        _state.value = current.copy(
            items = WooPosBookingsState.Content.Items.Loaded(newMap),
            selectedDetails = if (selectedId == updated.id) newDetailsViewState else current.selectedDetails
        )
    }

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        _state.value = when (val currentState = _state.value) {
            is WooPosBookingsState.Content -> currentState.copy(searchInputState = searchState)
            is WooPosBookingsState.Empty -> currentState.copy(searchInputState = searchState)
            is WooPosBookingsState.Error -> currentState.copy(searchInputState = searchState)
            is WooPosBookingsState.Loading -> currentState.copy(searchInputState = searchState)
        }
    }

    private fun performSearch(query: String, isRefreshing: Boolean = false) {
        cancelJobs()

        val currentSelectedDetails = (_state.value as? WooPosBookingsState.Content)?.selectedDetails
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            if (!isRefreshing) {
                _state.value = WooPosBookingsState.Content(
                    items = WooPosBookingsState.Content.Items.Searching,
                    pullToRefreshState = WooPosPullToRefreshState.Disabled,
                    searchInputState = _state.value.searchInputState,
                    selectedDetails = currentSelectedDetails,
                    paginationState = WooPosPaginationState.None,
                    dialogState = WooPosBookingsState.Content.DialogState.Hidden
                )
            }

            val mark = Monotonic.markNow()
            val result = ordersDataSource.searchOrders(query, forceRefreshRefunds = isRefreshing)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds
            ordersAnalyticsTracker.trackOrdersListSearchResultsFetched(elapsedMs)
            when (result) {
                is SearchOrdersResult.Error -> {
                    _state.value = WooPosBookingsState.Content(
                        items = WooPosBookingsState.Content.Items.Error(
                            title = resourceProvider.getString(R.string.woopos_search_orders_error_title),
                            message = resourceProvider.getString(R.string.woopos_search_orders_error_description)
                        ),
                        pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        searchInputState = _state.value.searchInputState,
                        selectedDetails = null,
                        paginationState = WooPosPaginationState.None,
                        dialogState = WooPosBookingsState.Content.DialogState.Hidden
                    )
                }

                is SearchOrdersResult.Success -> {
                    if (result.ordersWithRefunds.isEmpty()) {
                        _state.value = WooPosBookingsState.Content(
                            items = WooPosBookingsState.Content.Items.NothingFound(
                                title = resourceProvider.getString(R.string.woopos_search_orders_empty_title),
                                message = resourceProvider.getString(R.string.woopos_search_orders_empty_description)
                            ),
                            pullToRefreshState = WooPosPullToRefreshState.Enabled,
                            searchInputState = _state.value.searchInputState,
                            selectedDetails = null,
                            paginationState = WooPosPaginationState.None,
                            dialogState = WooPosBookingsState.Content.DialogState.Hidden
                        )
                    } else {
                        replaceOrders(result.ordersWithRefunds)
                    }
                }
            }
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

                        _state.value = WooPosBookingsState.Error(
                            message = result.message,
                            searchInputState = WooPosSearchInputState.Closed
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosBookingsState.Loading(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.ordersWithRefunds)
                        }
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosBookingsState.Empty(
                                searchInputState = WooPosSearchInputState.Closed
                            )
                        } else {
                            replaceOrders(result.ordersWithRefunds)
                        }
                    }
                }
            }
        }
    }

    private fun cancelJobs() {
        searchJob?.cancel()
        loadingJob?.cancel()
        loadingMoreOrdersJob?.cancel()
        sideLoadActionsJob?.cancel()
    }

    private suspend fun getOrComputeDetails(orderId: Long): WooPosBookingsState.OrderDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosBookingsState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosBookingsState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return orderDetailsMapper.mapOrderDetails(
            when (orderDetails) {
                is WooPosBookingsState.OrderDetailsViewState.Lazy -> orderDetails.order
                is WooPosBookingsState.OrderDetailsViewState.Computed -> {
                    loadedItems.items.keys.firstOrNull { it.id == orderId }?.let {
                        ordersDataSource.getOrderById(it.id).getOrNull()
                    } ?: error("Order $orderId not found")
                }
            },
            when (orderDetails) {
                is WooPosBookingsState.OrderDetailsViewState.Lazy -> orderDetails.refundResult
                is WooPosBookingsState.OrderDetailsViewState.Computed -> RefundsFetchResult.Error
            }
        )
    }

    private suspend fun getOrComputeDetailsWithoutActions(
        orderId: Long
    ): WooPosBookingsState.OrderDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosBookingsState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosBookingsState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return when (orderDetails) {
            is WooPosBookingsState.OrderDetailsViewState.Lazy ->
                orderDetailsMapper.mapOrderDetailsWithoutActions(orderDetails.order)

            is WooPosBookingsState.OrderDetailsViewState.Computed -> {
                orderDetails.details
            }
        }
    }

    private suspend fun replaceOrders(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val currentState = _state.value
        val currentFirstOrderId = (currentState as? WooPosBookingsState.Content)
            ?.let { it.items as? WooPosBookingsState.Content.Items.Loaded }
            ?.items?.keys?.firstOrNull()?.id

        val orders = ordersWithRefunds.keys.toList()
        val newFirstOrderId = orders.firstOrNull()?.id

        val currentSelectedId = (currentState as? WooPosBookingsState.Content)?.selectedDetails?.id
        val isSelectedOrderStillInList = currentSelectedId != null && orders.any { it.id == currentSelectedId }
        val newSelectedId =
            if (isSelectedOrderStillInList) {
                currentSelectedId
            } else {
                requireNotNull(newFirstOrderId) { "Content requires at least one order" }
            }
        val items = buildItemsMap(ordersWithRefunds, newSelectedId)
        val selectedEntry = items.entries.first { (item, _) -> item.isSelected }
        val selectedDetails = when (val details = selectedEntry.value) {
            is WooPosBookingsState.OrderDetailsViewState.Computed -> details.details
            is WooPosBookingsState.OrderDetailsViewState.Lazy -> error("Selected order should have computed details")
        }

        _state.value = WooPosBookingsState.Content(
            items = WooPosBookingsState.Content.Items.Loaded(
                items = items
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = selectedDetails,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState,
            dialogState = (currentState as? WooPosBookingsState.Content)?.dialogState
                ?: WooPosBookingsState.Content.DialogState.Hidden
        )

        if (currentFirstOrderId != null && currentFirstOrderId != newFirstOrderId) {
            _scrollToTopEvent.emit(Unit)
        }
    }

    private suspend fun appendOrders(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as WooPosBookingsState.Content
        val loadedItems = current.items as WooPosBookingsState.Content.Items.Loaded
        val currentSelectedId = loadedItems.items.entries.firstOrNull { it.key.isSelected }?.key?.id
        val newItems = buildItemsMap(ordersWithRefunds, currentSelectedId)
        val items = loadedItems.items + newItems

        _state.value = WooPosBookingsState.Content(
            items = WooPosBookingsState.Content.Items.Loaded(
                items = items
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = current.selectedDetails,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState,
            dialogState = current.dialogState
        )
    }

    private suspend fun buildItemsMap(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        selectedId: Long?
    ): Map<WooPosBookingsState.OrderItemViewState, WooPosBookingsState.OrderDetailsViewState> = coroutineScope {
        ordersWithRefunds.map { (order, refundResult) ->
            async {
                val item = orderItemMapper.mapOrderItem(order, selectedId)
                val details: WooPosBookingsState.OrderDetailsViewState = if (order.id == selectedId) {
                    val fullDetails = orderDetailsMapper.mapOrderDetails(order, refundResult)
                    WooPosBookingsState.OrderDetailsViewState.Computed(orderId = order.id, details = fullDetails)
                } else {
                    WooPosBookingsState.OrderDetailsViewState.Lazy(
                        orderId = order.id,
                        order = order,
                        refundResult = refundResult
                    )
                }

                item to details
            }
        }.awaitAll().toMap()
    }
}
