package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
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
import java.util.Locale
import javax.inject.Inject
import kotlin.time.TimeSource.Monotonic

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
    private val getProductById: WooPosGetProductById,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val formatPrice: WooPosFormatPrice,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(
        WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
    )
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

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
        loadOrders()
    }

    fun onOrderSelected(orderId: Long) {
        val current = _state.value as? WooPosOrdersState.Content ?: return
        val loadedItems = current.items as? WooPosOrdersState.Content.Items.Loaded ?: return

        val keys = loadedItems.items.keys.toList()
        val position = keys.indexOfFirst { it.id == orderId }.coerceAtLeast(0)
        val selectedItem = keys.firstOrNull { it.id == orderId }

        selectedItem?.let {
            viewModelScope.launch {
                ordersAnalyticsTracker.trackOrdersListRowTapped(
                    orderId = it.id,
                    orderStatus = it.statusSlug,
                    listPosition = position,
                    createdAtMillis = it.createdAtMillis
                )
                ordersAnalyticsTracker.trackOrderDetailsLoaded(
                    orderId = it.id,
                    orderStatus = it.statusSlug,
                    createdAtMillis = it.createdAtMillis
                )
            }
        }

        viewModelScope.launch {
            val details = getOrComputeDetails(orderId)

            val updatedItems = loadedItems.items.mapKeys { (item, _) ->
                item.copy(isSelected = item.id == orderId)
            }.mapValues { (item, orderDetails) ->
                if (item.id == orderId && orderDetails is OrderDetailsViewState.Lazy) {
                    OrderDetailsViewState.Computed(orderId = orderId, details = details)
                } else {
                    orderDetails
                }
            }

            _state.value = current.copy(
                items = WooPosOrdersState.Content.Items.Loaded(
                    items = updatedItems
                ),
                selectedDetails = details
            )
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
            loadOrders()
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

    fun onOrdersEmptyActionClicked() {
        viewModelScope.launch {
            _openUrlEvent.emit(AppUrls.URL_LEARN_MORE_ORDERS)
        }
    }

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
        loadOrders()
    }

    fun onSearchErrorRetry() {
        val query = currentSearchQuery
        if (!query.isNullOrEmpty()) {
            performSearch(query)
        }
    }

    @Suppress("ReturnCount")
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
        val selectedOrderId = current.selectedDetails.id

        viewModelScope.launch {
            ordersDataSource.refreshOrderById(selectedOrderId)
                .onSuccess { applyOrderUpdate(it) }
        }
    }

    private suspend fun applyOrderUpdate(updated: Order) {
        val current = _state.value as? WooPosOrdersState.Content ?: return
        val loaded = current.items as? WooPosOrdersState.Content.Items.Loaded ?: return

        val refundResult = retrieveOrderRefunds(updated).fold(
            onSuccess = { refunds -> RefundFetchResult.Success(refunds) },
            onFailure = { RefundFetchResult.Error }
        )

        val selectedId = loaded.items.keys.firstOrNull { it.isSelected }?.id
        val newItem = mapOrderItem(updated, selectedId)
        val newDetailsViewState = mapOrderDetails(updated, refundResult)
        val newDetails = OrderDetailsViewState.Computed(
            orderId = updated.id,
            details = newDetailsViewState
        )

        val newMap = loaded.items.entries.associate { (item, details) ->
            if (item.id == updated.id) newItem to newDetails else item to details
        }

        _state.value = current.copy(
            items = WooPosOrdersState.Content.Items.Loaded(newMap),
            selectedDetails = if (selectedId == updated.id) newDetailsViewState else current.selectedDetails
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

        val currentSelectedDetails = (_state.value as? WooPosOrdersState.Content)?.selectedDetails!!
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            if (!isRefreshing) {
                _state.value = WooPosOrdersState.Content(
                    items = WooPosOrdersState.Content.Items.Searching,
                    pullToRefreshState = WooPosPullToRefreshState.Disabled,
                    searchInputState = _state.value.searchInputState,
                    selectedDetails = currentSelectedDetails,
                    paginationState = WooPosPaginationState.None
                )
            }

            val mark = Monotonic.markNow()
            val result = ordersDataSource.searchOrders(query)
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
                        selectedDetails = currentSelectedDetails,
                        paginationState = WooPosPaginationState.None
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
                            selectedDetails = currentSelectedDetails,
                            paginationState = WooPosPaginationState.None
                        )
                    } else {
                        replaceOrders(result.ordersWithRefunds)
                    }
                }
            }
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
    }

    private suspend fun getOrComputeDetails(orderId: Long): OrderDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosOrdersState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosOrdersState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return when (orderDetails) {
            is OrderDetailsViewState.Lazy -> mapOrderDetails(orderDetails.order, orderDetails.refundResult)
            is OrderDetailsViewState.Computed -> orderDetails.details
        }
    }

    private suspend fun replaceOrders(
        ordersWithRefunds: Map<Order, RefundFetchResult>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val orders = ordersWithRefunds.keys.toList()
        val newSelectedId = requireNotNull(orders.firstOrNull()?.id) { "Content requires at least one order" }
        val items = buildItemsMap(ordersWithRefunds, newSelectedId)
        val selectedEntry = items.entries.first { (item, _) -> item.isSelected }
        val selectedDetails = when (val details = selectedEntry.value) {
            is OrderDetailsViewState.Computed -> details.details
            is OrderDetailsViewState.Lazy -> error("Selected order should have computed details")
        }

        _state.value = WooPosOrdersState.Content(
            items = WooPosOrdersState.Content.Items.Loaded(
                items = items
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = selectedDetails,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState
        )
    }

    private suspend fun appendOrders(
        ordersWithRefunds: Map<Order, RefundFetchResult>,
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
            searchInputState = _state.value.searchInputState
        )
    }

    private suspend fun buildItemsMap(
        ordersWithRefunds: Map<Order, RefundFetchResult>,
        selectedId: Long?
    ): Map<OrderItemViewState, OrderDetailsViewState> = coroutineScope {
        ordersWithRefunds.map { (order, refundResult) ->
            async {
                val item = mapOrderItem(order, selectedId)
                val details: OrderDetailsViewState = if (order.id == selectedId) {
                    val fullDetails = mapOrderDetails(order, refundResult)
                    OrderDetailsViewState.Computed(orderId = order.id, details = fullDetails)
                } else {
                    OrderDetailsViewState.Lazy(
                        orderId = order.id,
                        order = order,
                        refundResult = refundResult
                    )
                }

                item to details
            }
        }.awaitAll().toMap()
    }

    private suspend fun mapOrderItem(order: Order, selectedId: Long?): OrderItemViewState {
        val statusText = order.status.localizedLabel(resourceProvider, locale)

        return OrderItemViewState(
            id = order.id,
            title = "#${order.number}",
            date = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            total = formatPrice(order.total),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            isSelected = order.id == selectedId,
            status = PosOrderStatus(
                text = statusText,
                colorKey = OrderStatusColorKey.fromStatus(order.status)
            ),
            statusSlug = order.status.toString(),
            createdAtMillis = order.dateCreated.time
        )
    }
    
    private suspend fun mapOrderDetails(
        order: Order,
        refundResult: RefundFetchResult
    ): OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = mapOrderStatus(order)
        val lineItems = buildLineItems(order)
        val refundInfo = buildRefundInfo(order, refundResult)
        val breakdown = buildTotalsBreakdown(order, refundInfo)

        OrderDetailsViewState.Computed.Details(
            id = order.id,
            number = "#${order.number}",
            dateTime = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            status = status,
            lineItems = lineItems,
            breakdown = breakdown,
            total = formatPrice(order.total),
            totalPaid = formatPrice(order.total),
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() }
        )
    }

    private fun mapOrderStatus(order: Order): PosOrderStatus {
        val statusText = order.status.localizedLabel(resourceProvider, locale)
        return PosOrderStatus(
            text = statusText,
            colorKey = OrderStatusColorKey.fromStatus(order.status)
        )
    }

    private suspend fun buildLineItems(
        order: Order
    ): List<OrderDetailsViewState.Computed.Details.LineItemRow> = coroutineScope {
        order.items.map { item ->
            async {
                val unitPrice =
                    if (item.quantity == 0f) {
                        item.total
                    } else {
                        item.total / item.quantity.toBigDecimal()
                    }
                val product = getProductById(item.productId)
                OrderDetailsViewState.Computed.Details.LineItemRow(
                    id = item.itemId,
                    name = item.name,
                    qtyAndUnitPrice = "${item.quantity.toInt()} x ${formatPrice(unitPrice)}",
                    lineTotal = formatPrice(item.total),
                    imageUrl = product?.firstImageUrl
                )
            }
        }.awaitAll()
    }

    private data class RefundInfo(
        val refundAmounts: List<String>,
        val totalRefunded: BigDecimal
    )

    private suspend fun buildRefundInfo(
        order: Order,
        refundResult: RefundFetchResult
    ): RefundInfo {
        return when (refundResult) {
            is RefundFetchResult.Success -> {
                val amounts = refundResult.refunds.map { "-${formatPrice(it.amount)}" }
                val total = refundResult.refunds.sumOf { it.amount }
                RefundInfo(amounts, total)
            }
            is RefundFetchResult.Error -> {
                val amounts =
                    if (order.refundTotal > BigDecimal.ZERO) {
                        listOf(resourceProvider.getString(R.string.woopos_orders_details_refund_error))
                    } else {
                        emptyList()
                    }
                RefundInfo(amounts, BigDecimal.ZERO)
            }
        }
    }

    private suspend fun buildTotalsBreakdown(
        order: Order,
        refundInfo: RefundInfo
    ): OrderDetailsViewState.Computed.Details.TotalsBreakdown {
        val netPayment = if (refundInfo.totalRefunded > BigDecimal.ZERO) {
            formatPrice(order.total - refundInfo.totalRefunded)
        } else {
            null
        }

        val discountCode = order.couponLines.firstOrNull()?.code

        return OrderDetailsViewState.Computed.Details.TotalsBreakdown(
            products = formatPrice(order.productsTotal),
            discount = order.discountTotal.takeIf { it != BigDecimal.ZERO }?.let { "-${formatPrice(it)}" },
            discountCode = discountCode,
            taxes = formatPrice(order.totalTax),
            shipping = order.shippingTotal.takeIf { it != BigDecimal.ZERO }?.let { formatPrice(it) },
            refunds = refundInfo.refundAmounts,
            netPayment = netPayment
        )
    }
}

private fun Order.Status.localizedLabel(resourceProvider: ResourceProvider, locale: Locale): String {
    return when (this) {
        Order.Status.Cancelled -> resourceProvider.getString(R.string.woopos_orders_status_cancelled)
        Order.Status.Completed -> resourceProvider.getString(R.string.woopos_orders_status_completed)
        is Order.Status.Custom ->
            value.replaceFirstChar { it.titlecase(locale) }.replace("-", " ")
        Order.Status.Failed -> resourceProvider.getString(R.string.woopos_orders_status_failed)
        Order.Status.OnHold -> resourceProvider.getString(R.string.woopos_orders_status_on_hold)
        Order.Status.Pending -> resourceProvider.getString(R.string.woopos_orders_status_pending)
        Order.Status.Processing -> resourceProvider.getString(R.string.woopos_orders_status_processing)
        Order.Status.Refunded -> resourceProvider.getString(R.string.woopos_orders_status_refunded)
    }
}
