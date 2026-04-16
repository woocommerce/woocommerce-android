package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.SavedStateHandle
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
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.BookingInfo
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState
import com.woocommerce.android.ui.woopos.orders.details.WooPosBookingInfoMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundRowData
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
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
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.TimeSource.Monotonic

@Suppress("LargeClass")
@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderItemMapper: WooPosOrderItemMapper,
    private val orderDetailsMapper: WooPosOrderDetailsMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosOrderActionsProvider,
    private val bookingInfoMapper: WooPosBookingInfoMapper,
    private val formatPrice: WooPosFormatPrice,
) : ViewModel() {

    private val singleOrderId: Long? = savedStateHandle.get<Long>(ORDERS_ROUTE_ORDER_ID_KEY)

    val isSingleOrderMode: Boolean = singleOrderId != null

    private val _state = MutableStateFlow<WooPosOrdersState>(
        WooPosOrdersState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
    )
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var loadingJob: Job? = null
    private var loadingMoreOrdersJob: Job? = null
    private var sideLoadJob: Job? = null
    private var refreshOrderJob: Job? = null

    private var cachedRefundData: CachedRefundData? = null

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
        if (singleOrderId != null) {
            viewModelScope.launch { loadSingleOrderDetail(singleOrderId) }
        } else {
            loadOrders()
        }
    }

    private suspend fun loadSingleOrderDetail(orderId: Long) {
        val order = ordersDataSource.getOrderById(orderId).getOrElse {
            _state.value = WooPosOrdersState.Error(
                message = resourceProvider.getString(R.string.woopos_orders_loading_error_message),
                searchInputState = WooPosSearchInputState.Closed
            )
            return
        }
        val details = orderDetailsMapper.mapOrderDetailsWithoutActions(order)
        _state.value = WooPosOrdersState.Content(
            items = WooPosOrdersState.Content.Items.Loaded(emptyMap()),
            pullToRefreshState = WooPosPullToRefreshState.Disabled,
            searchInputState = WooPosSearchInputState.Closed,
            selectedDetails = details,
            paginationState = WooPosPaginationState.None,
            dialogState = WooPosOrdersState.Content.DialogState.Hidden
        )
        ordersAnalyticsTracker.trackOrderDetailsLoaded(
            orderId = orderId,
            orderStatus = order.status.value,
            createdAtMillis = order.dateCreated.time
        )
        sideLoadActionsAndRefundData(orderId, order)
    }

    fun onUIEvent(event: WooPosOrdersUIEvent) {
        when (event) {
            is WooPosOrdersUIEvent.OrderActionClicked -> handleActionClicked(event.action)
            is WooPosOrdersUIEvent.ViewRefundDetailsClicked -> onViewRefundDetailsClicked(event.refundIndex)
        }
    }

    private fun handleActionClicked(action: WooPosOrdersState.OrderAction) {
        when (action) {
            is WooPosOrdersState.OrderAction.EmailReceipt -> onEmailReceiptButtonClicked(action.orderId)
            is WooPosOrdersState.OrderAction.IssueRefund -> onIssueRefundButtonClicked(action.orderId)
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

    private fun sideLoadActionsAndRefundData(orderId: Long, order: Order) {
        sideLoadJob?.cancel()
        sideLoadJob = viewModelScope.launch {
            val refundsResult = retrieveOrderRefunds(order = order, forceRefresh = true).fold(
                onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
                onFailure = { RefundsFetchResult.Error }
            )

            val stateAfterRefundsFetch = _state.value as? WooPosOrdersState.Content
            if (stateAfterRefundsFetch?.selectedDetails?.id != orderId) {
                return@launch
            }

            val actions = orderActionsProvider.getAvailableActions(order)
            val refundInfo = refundInfoBuilder.buildRefundInfo(order, refundsResult)
            val updatedBreakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
            val refundedLineItems = orderDetailsMapper.buildRefundedLineItems(order, refundsResult)
            val nonRefundedLineItems = orderDetailsMapper.buildNonRefundedLineItems(order, refundsResult)

            cacheRefundData(orderId, refundInfo.refundRows, order)

            val updatedState = _state.value as? WooPosOrdersState.Content ?: return@launch
            if (updatedState.selectedDetails?.id == orderId &&
                updatedState.selectedDetails.actionsState is WooPosOrdersState.OrderActionsState.Loading
            ) {
                _state.value = updatedState.withUpdatedDetails(orderId) { details ->
                    details.copy(
                        actionsState = WooPosOrdersState.OrderActionsState.Loaded(actions),
                        breakdown = updatedBreakdown,
                        lineItems = LineItemsState.Loaded(nonRefundedLineItems),
                        refundedLineItems = LineItemsState.Loaded(refundedLineItems)
                    )
                }

                val stateAfterUpdate = _state.value as? WooPosOrdersState.Content
                val updatedDetails = stateAfterUpdate?.selectedDetails
                if (updatedDetails != null) {
                    sideLoadBookings(orderId, updatedDetails)
                }
            }
        }
    }

    private fun sideLoadBookings(
        orderId: Long,
        details: WooPosOrdersState.OrderDetailsViewState.Computed.Details
    ) {
        val loadedItems = (details.lineItems as? LineItemsState.Loaded)?.items ?: return
        val loadingItems = loadedItems.filter { it.bookingInfo is BookingInfo.Loading }
        if (loadingItems.isEmpty()) return

        viewModelScope.launch {
            val results = coroutineScope {
                loadingItems.map { item ->
                    async {
                        val bookingId = (item.bookingInfo as BookingInfo.Loading).bookingId
                        item.id to bookingInfoMapper.fetchBookingInfo(bookingId)
                    }
                }.awaitAll()
            }.toMap()

            val currentState = _state.value as? WooPosOrdersState.Content ?: return@launch
            if (currentState.selectedDetails?.id != orderId) return@launch

            _state.value = currentState.withUpdatedDetails(orderId) { details ->
                val currentItems = (details.lineItems as? LineItemsState.Loaded)?.items
                    ?: return@withUpdatedDetails details
                details.copy(
                    lineItems = LineItemsState.Loaded(
                        currentItems.map { lineItem ->
                            results[lineItem.id]?.let { lineItem.copy(bookingInfo = it) } ?: lineItem
                        }
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
        if (currentState !is WooPosOrdersState.Content ||
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
        val currentState = _state.value as? WooPosOrdersState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosOrdersState.Content.DialogState.IssueRefund(
                orderId = orderId
            )
        )
    }

    fun onIssueRefundDialogDismissed() {
        val currentState = _state.value as? WooPosOrdersState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosOrdersState.Content.DialogState.Hidden
        )
        refreshSelectedOrder()
    }

    private fun onViewRefundDetailsClicked(refundIndex: Int) {
        val cached = cachedRefundData ?: return
        val rowData = cached.rows.getOrNull(refundIndex) ?: return
        val order = cached.order
        if (_state.value !is WooPosOrdersState.Content) return

        viewModelScope.launch {
            val items = orderDetailsMapper.buildLineItemsForSingleRefund(order, rowData.refund)
            val itemsSubtotal = formatPrice(
                rowData.refund.items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total },
                order.currency
            )
            val tax = formatPrice(
                rowData.refund.items.fold(BigDecimal.ZERO) { acc, item -> acc + item.totalTax },
                order.currency
            )
            val refundTotal = formatPrice(rowData.refund.amount, order.currency)

            val updatedState = _state.value as? WooPosOrdersState.Content ?: return@launch
            if (updatedState.selectedDetails?.id != cached.orderId) return@launch
            _state.value = updatedState.copy(
                dialogState = WooPosOrdersState.Content.DialogState.RefundDetails(
                    label = rowData.label,
                    items = items,
                    itemsSubtotalLabel = resourceProvider.getQuantityString(
                        quantity = items.size,
                        default = R.string.woopos_orders_details_refund_items_subtotal_other,
                        one = R.string.woopos_orders_details_refund_items_subtotal_one,
                    ),
                    itemsSubtotalAmount = itemsSubtotal,
                    tax = tax,
                    refundTotal = refundTotal,
                    paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
                )
            )
        }
    }

    fun onRefundDetailsDialogDismissed() {
        val currentState = _state.value as? WooPosOrdersState.Content ?: return
        _state.value = currentState.copy(
            dialogState = WooPosOrdersState.Content.DialogState.Hidden
        )
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
        if (singleOrderId != null) {
            viewModelScope.launch { loadSingleOrderDetail(singleOrderId) }
        } else {
            loadOrders()
        }
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
            is WooPosOrdersState.Content -> currentState.copy(paginationState = WooPosPaginationState.Loading)
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
        val current = _state.value as? WooPosOrdersState.Content ?: return
        val selectedOrderId = current.selectedDetails?.id ?: return

        sideLoadJob?.cancel()
        refreshOrderJob?.cancel()
        refreshOrderJob = viewModelScope.launch {
            ordersDataSource.refreshOrderById(selectedOrderId)
                .onSuccess { applyOrderUpdate(it) }
        }
    }

    private suspend fun applyOrderUpdate(updated: Order) {
        val current = _state.value as? WooPosOrdersState.Content ?: return
        if (current.items !is WooPosOrdersState.Content.Items.Loaded) return

        val historicalRefundsResult = retrieveOrderRefunds(updated, forceRefresh = true).fold(
            onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
            onFailure = { RefundsFetchResult.Error }
        )

        val freshState = _state.value as? WooPosOrdersState.Content ?: return
        val freshLoaded = freshState.items as? WooPosOrdersState.Content.Items.Loaded ?: return

        val refundInfo = refundInfoBuilder.buildRefundInfo(updated, historicalRefundsResult)
        cacheRefundData(updated.id, refundInfo.refundRows, updated)

        val selectedId = freshLoaded.items.keys.firstOrNull { it.isSelected }?.id
        val newItem = orderItemMapper.mapOrderItem(updated, selectedId)
        val newDetailsViewState = orderDetailsMapper.mapOrderDetails(updated, historicalRefundsResult)
        val newDetails = WooPosOrdersState.OrderDetailsViewState.Computed(
            orderId = updated.id,
            details = newDetailsViewState
        )

        val newMap = freshLoaded.items.entries.associate { (item, details) ->
            if (item.id == updated.id) newItem to newDetails else item to details
        }

        val shouldUpdateDetails = freshState.selectedDetails?.id == updated.id
        _state.value = freshState.copy(
            items = WooPosOrdersState.Content.Items.Loaded(newMap),
            selectedDetails = if (shouldUpdateDetails) newDetailsViewState else freshState.selectedDetails
        )
    }

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        _state.value = when (val currentState = _state.value) {
            is WooPosOrdersState.Content -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Empty -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Error -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Loading -> currentState.copy(searchInputState = searchState)
        }
    }

    private fun performSearch(query: String, isRefreshing: Boolean = false) {
        cancelJobs()

        val currentSelectedDetails = (_state.value as? WooPosOrdersState.Content)?.selectedDetails
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            if (!isRefreshing) {
                _state.value = WooPosOrdersState.Content(
                    items = WooPosOrdersState.Content.Items.Searching,
                    pullToRefreshState = WooPosPullToRefreshState.Disabled,
                    searchInputState = _state.value.searchInputState,
                    selectedDetails = currentSelectedDetails,
                    paginationState = WooPosPaginationState.None,
                    dialogState = WooPosOrdersState.Content.DialogState.Hidden
                )
            }

            val mark = Monotonic.markNow()
            val result = ordersDataSource.searchOrders(query, forceRefreshRefunds = isRefreshing)
            val elapsedMs = mark.elapsedNow().inWholeMilliseconds
            ordersAnalyticsTracker.trackOrdersListSearchResultsFetched(elapsedMs)
            when (result) {
                is SearchOrdersResult.Error -> {
                    _state.value = WooPosOrdersState.Content(
                        items = WooPosOrdersState.Content.Items.Error(
                            title = resourceProvider.getString(R.string.woopos_search_orders_error_title),
                            message = resourceProvider.getString(R.string.woopos_search_orders_error_description)
                        ),
                        pullToRefreshState = WooPosPullToRefreshState.Enabled,
                        searchInputState = _state.value.searchInputState,
                        selectedDetails = null,
                        paginationState = WooPosPaginationState.None,
                        dialogState = WooPosOrdersState.Content.DialogState.Hidden
                    )
                }

                is SearchOrdersResult.Success -> {
                    if (result.ordersWithRefunds.isEmpty()) {
                        _state.value = WooPosOrdersState.Content(
                            items = WooPosOrdersState.Content.Items.NothingFound(
                                title = resourceProvider.getString(R.string.woopos_search_orders_empty_title),
                                message = resourceProvider.getString(R.string.woopos_search_orders_empty_description)
                            ),
                            pullToRefreshState = WooPosPullToRefreshState.Enabled,
                            searchInputState = _state.value.searchInputState,
                            selectedDetails = null,
                            paginationState = WooPosPaginationState.None,
                            dialogState = WooPosOrdersState.Content.DialogState.Hidden
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

                        _state.value = WooPosOrdersState.Error(
                            message = result.message,
                            searchInputState = WooPosSearchInputState.Closed
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosOrdersState.Loading(
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
                            _state.value = WooPosOrdersState.Empty(
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
        sideLoadJob?.cancel()
        refreshOrderJob?.cancel()
    }

    private suspend fun getOrComputeDetails(orderId: Long): WooPosOrdersState.OrderDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosOrdersState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosOrdersState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return when (orderDetails) {
            is WooPosOrdersState.OrderDetailsViewState.Computed -> orderDetails.details
            is WooPosOrdersState.OrderDetailsViewState.Lazy ->
                orderDetailsMapper.mapOrderDetails(orderDetails.order, orderDetails.refundResult)
        }
    }

    private suspend fun getOrComputeDetailsWithoutActions(
        orderId: Long
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosOrdersState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosOrdersState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return when (orderDetails) {
            is WooPosOrdersState.OrderDetailsViewState.Lazy ->
                orderDetailsMapper.mapOrderDetailsWithoutActions(orderDetails.order)
            is WooPosOrdersState.OrderDetailsViewState.Computed -> {
                orderDetails.details
            }
        }
    }

    private suspend fun replaceOrders(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val currentState = _state.value
        val currentFirstOrderId = (currentState as? WooPosOrdersState.Content)
            ?.let { it.items as? WooPosOrdersState.Content.Items.Loaded }
            ?.items?.keys?.firstOrNull()?.id

        val orders = ordersWithRefunds.keys.toList()
        val newFirstOrderId = orders.firstOrNull()?.id

        val currentSelectedId = (currentState as? WooPosOrdersState.Content)?.selectedDetails?.id
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
            is WooPosOrdersState.OrderDetailsViewState.Computed -> details.details
            is WooPosOrdersState.OrderDetailsViewState.Lazy -> error("Selected order should have computed details")
        }

        val selectedOrder = ordersWithRefunds.keys.firstOrNull { it.id == newSelectedId }
        val selectedRefundResult = selectedOrder?.let { ordersWithRefunds[it] }
        if (selectedOrder != null && selectedRefundResult != null) {
            val refundInfo = refundInfoBuilder.buildRefundInfo(selectedOrder, selectedRefundResult)
            cacheRefundData(newSelectedId, refundInfo.refundRows, selectedOrder)
        }

        _state.value = WooPosOrdersState.Content(
            items = WooPosOrdersState.Content.Items.Loaded(
                items = items
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = selectedDetails,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState,
            dialogState = (currentState as? WooPosOrdersState.Content)?.dialogState
                ?: WooPosOrdersState.Content.DialogState.Hidden
        )

        if (currentFirstOrderId != null && currentFirstOrderId != newFirstOrderId) {
            _scrollToTopEvent.emit(Unit)
        }

        sideLoadBookings(newSelectedId, selectedDetails)
    }

    private suspend fun appendOrders(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as WooPosOrdersState.Content
        val loadedItems = current.items as WooPosOrdersState.Content.Items.Loaded
        val currentSelectedId = loadedItems.items.entries.firstOrNull { it.key.isSelected }?.key?.id
        val newItems = buildItemsMap(ordersWithRefunds, currentSelectedId)
        val items = loadedItems.items + newItems

        _state.value = WooPosOrdersState.Content(
            items = WooPosOrdersState.Content.Items.Loaded(
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
    ): Map<WooPosOrdersState.OrderItemViewState, WooPosOrdersState.OrderDetailsViewState> = coroutineScope {
        ordersWithRefunds.map { (order, refundResult) ->
            async {
                val item = orderItemMapper.mapOrderItem(order, selectedId)
                val details: WooPosOrdersState.OrderDetailsViewState = if (order.id == selectedId) {
                    val fullDetails = orderDetailsMapper.mapOrderDetails(order, refundResult)
                    WooPosOrdersState.OrderDetailsViewState.Computed(orderId = order.id, details = fullDetails)
                } else {
                    WooPosOrdersState.OrderDetailsViewState.Lazy(
                        orderId = order.id,
                        order = order,
                        refundResult = refundResult
                    )
                }

                item to details
            }
        }.awaitAll().toMap()
    }

    private fun WooPosOrdersState.Content.withUpdatedDetails(
        orderId: Long,
        transform: (WooPosOrdersState.OrderDetailsViewState.Computed.Details) ->
        WooPosOrdersState.OrderDetailsViewState.Computed.Details
    ): WooPosOrdersState.Content {
        var transformedDetails: WooPosOrdersState.OrderDetailsViewState.Computed.Details? = null

        val updatedSelectedDetails = if (selectedDetails?.id == orderId) {
            transform(selectedDetails).also { transformedDetails = it }
        } else {
            selectedDetails
        }
        val loadedItems = items as? WooPosOrdersState.Content.Items.Loaded
        val updatedItems = loadedItems?.items?.map { (orderItem, orderDetails) ->
            if (orderItem.id == orderId && orderDetails is WooPosOrdersState.OrderDetailsViewState.Computed) {
                val newDetails = transformedDetails ?: transform(orderDetails.details).also {
                    transformedDetails = it
                }
                orderItem to orderDetails.copy(details = newDetails)
            } else {
                orderItem to orderDetails
            }
        }?.toMap()

        return copy(
            items = if (updatedItems != null) {
                WooPosOrdersState.Content.Items.Loaded(updatedItems)
            } else {
                items
            },
            selectedDetails = updatedSelectedDetails
        )
    }

    private fun cacheRefundData(orderId: Long, refundRows: List<RefundRowData>, order: Order) {
        cachedRefundData = CachedRefundData(orderId = orderId, rows = refundRows, order = order)
    }

    private data class CachedRefundData(
        val orderId: Long,
        val rows: List<RefundRowData>,
        val order: Order,
    )
}
