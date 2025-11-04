package com.woocommerce.android.ui.woopos.orders

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosGetOrderRefundsByOrderId
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
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
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlin.time.TimeSource.Monotonic

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
    private val getProductById: WooPosGetProductById,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val formatPrice: WooPosFormatPrice,
    private val getOrderRefunds: WooPosGetOrderRefundsByOrderId,
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

        val updatedItems = loadedItems.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == orderId)
        }

        val selectedEntry = updatedItems.entries.first { (item, _) -> item.isSelected }

        _state.value = current.copy(
            items = WooPosOrdersState.Content.Items.Loaded(
                items = updatedItems
            ),
            selectedDetails = selectedEntry.value
        )
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

        val selectedId = loaded.items.keys.firstOrNull { it.isSelected }?.id
        val newItem = mapOrderItem(updated, selectedId)
        val newDetails = mapOrderDetails(updated)

        val newMap = loaded.items.entries.associate { (item, details) ->
            if (item.id == updated.id) newItem to newDetails else item to details
        }

        _state.value = current.copy(
            items = WooPosOrdersState.Content.Items.Loaded(newMap),
            selectedDetails = if (selectedId == updated.id) newDetails else current.selectedDetails
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
                    if (result.orders.isEmpty()) {
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
                        replaceOrders(result.orders)
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
                        if (result.orders.isEmpty()) {
                            _state.value = WooPosOrdersState.Loading(
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
                            _state.value = WooPosOrdersState.Empty(
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

    private fun cancelJobs() {
        searchJob?.cancel()
        loadingJob?.cancel()
        loadingMoreOrdersJob?.cancel()
    }

    private suspend fun replaceOrders(
        orders: List<Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val selectedId = requireNotNull(orders.firstOrNull()?.id) { "Content requires at least one order" }

        val elapsed = measureTimeMillis {
            val items = buildItemsMap(orders, selectedId)
            val selectedEntry = items.entries.first { (item, _) -> item.isSelected }

            _state.value = WooPosOrdersState.Content(
                items = WooPosOrdersState.Content.Items.Loaded(
                    items = items
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                selectedDetails = selectedEntry.value,
                paginationState = paginationState,
                searchInputState = _state.value.searchInputState
            )
        }

        Log.d("WooPOS", "replaceOrders: buildItemsMap() took ${elapsed}ms for ${orders.size} orders")
    }

    private suspend fun appendOrders(
        orders: List<Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as WooPosOrdersState.Content
        val loadedItems = current.items as WooPosOrdersState.Content.Items.Loaded
        val currentSelectedId = loadedItems.items.entries.firstOrNull { it.key.isSelected }?.key?.id
        val newItems = buildItemsMap(orders, currentSelectedId)
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
        orders: List<Order>,
        selectedId: Long?
    ): Map<OrderItemViewState, OrderDetailsViewState> {
        val totalElapsed = measureTimeMillis {
            return orders.associate { order ->
                var item: OrderItemViewState
                var details: OrderDetailsViewState

                val itemTime = measureTimeMillis {
                    item = mapOrderItem(order, selectedId)
                }

                val detailsTime = measureTimeMillis {
                    details = mapOrderDetails(order)
                }

                Log.d(
                    "WooPOS",
                    "Order ${order.id} → mapOrderItem: ${itemTime}ms, mapOrderDetails: ${detailsTime}ms"
                )

                item to details
            }
        }

        Log.d("WooPOS", "buildItemsMap() total time: ${totalElapsed}ms for ${orders.size} orders")

        // the associate block already returned, so we re-measure outside the lambda
        return orders.associate { order ->
            val item = mapOrderItem(order, selectedId)
            val details = mapOrderDetails(order)
            item to details
        }
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

    private suspend fun mapOrderDetails(order: Order): OrderDetailsViewState {
        val tag = "WooPOS-Details"
        val totalStart = System.currentTimeMillis()

        val statusText = order.status.localizedLabel(resourceProvider, locale)
        val status = PosOrderStatus(
            text = statusText,
            colorKey = OrderStatusColorKey.fromStatus(order.status)
        )

        // 1️⃣ Line items + getProductById + price formatting
        val lineItemsStart = System.currentTimeMillis()
        val lineItems = order.items.map { item ->
            val unitPrice = if (item.quantity == 0f) {
                item.total
            } else {
                item.total / item.quantity.toBigDecimal()
            }

            val productStart = System.currentTimeMillis()
            val product = getProductById(item.productId)
            val productDuration = System.currentTimeMillis() - productStart
            Log.d(tag, "order ${order.id} - getProductById(${item.productId}) took ${productDuration}ms")

            val priceFormatStart = System.currentTimeMillis()
            val unitPriceFormatted = formatPrice(unitPrice)
            val totalFormatted = formatPrice(item.total)
            val priceFormatDuration = System.currentTimeMillis() - priceFormatStart
            Log.d(tag, "order ${order.id} - formatPrice(line item ${item.itemId}) took ${priceFormatDuration}ms")

            OrderDetailsViewState.LineItemRow(
                id = item.itemId,
                name = item.name,
                qtyAndUnitPrice = "${item.quantity.toInt()} x $unitPriceFormatted",
                lineTotal = totalFormatted,
                imageUrl = product?.firstImageUrl
            )
        }
        val lineItemsDuration = System.currentTimeMillis() - lineItemsStart
        Log.d(tag, "order ${order.id} - building line items took ${lineItemsDuration}ms")

        // 2️⃣ Refunds
        val refundsStart = System.currentTimeMillis()
        val refunds = getOrderRefunds(order.id)
        val refundsDuration = System.currentTimeMillis() - refundsStart
        Log.d(tag, "order ${order.id} - getOrderRefunds took ${refundsDuration}ms")

        val refundAmounts = refunds.map {
            val start = System.currentTimeMillis()
            val formatted = formatPrice(it.amount)
            Log.d(tag, "order ${order.id} - formatPrice(refund ${it.id}) took ${System.currentTimeMillis() - start}ms")
            "-$formatted"
        }

        val totalRefunded = refunds.sumOf { it.amount }
        val netPayment = if (totalRefunded > BigDecimal.ZERO) {
            val start = System.currentTimeMillis()
            val formatted = formatPrice(order.total - totalRefunded)
            Log.d(tag, "order ${order.id} - formatPrice(netPayment) took ${System.currentTimeMillis() - start}ms")
            formatted
        } else null

        // 3️⃣ Breakdown + price formatting
        val breakdownStart = System.currentTimeMillis()

        suspend fun timedFormatPrice(label: String, amount: BigDecimal): String {
            val start = System.currentTimeMillis()
            val result = formatPrice(amount)
            Log.d(tag, "order ${order.id} - formatPrice($label) took ${System.currentTimeMillis() - start}ms")
            return result
        }

        val discountCode = order.couponLines.firstOrNull()?.code
        val breakdown = OrderDetailsViewState.TotalsBreakdown(
            products = timedFormatPrice("products", order.productsTotal),
            discount = order.discountTotal.takeIf { it != BigDecimal.ZERO }?.let {
                "-${timedFormatPrice("discount", it)}"
            },
            discountCode = discountCode,
            taxes = timedFormatPrice("taxes", order.totalTax),
            shipping = order.shippingTotal.takeIf { it != BigDecimal.ZERO }?.let {
                timedFormatPrice("shipping", it)
            },
            refunds = refundAmounts,
            netPayment = netPayment
        )
        val breakdownDuration = System.currentTimeMillis() - breakdownStart
        Log.d(tag, "order ${order.id} - building breakdown took ${breakdownDuration}ms")

        // 4️⃣ Date formatting
        val dateFormatStart = System.currentTimeMillis()
        val formattedDate = order.dateCreated.formatToMMMddYYYYAtHHmm(
            atWord = resourceProvider.getString(R.string.date_time_connector)
        )
        val dateFormatDuration = System.currentTimeMillis() - dateFormatStart
        Log.d(tag, "order ${order.id} - date formatting took ${dateFormatDuration}ms")

        // 5️⃣ Build result
        val result = OrderDetailsViewState(
            id = order.id,
            number = "#${order.number}",
            dateTime = formattedDate,
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            status = status,
            lineItems = lineItems,
            breakdown = breakdown,
            total = timedFormatPrice("total", order.total),
            totalPaid = timedFormatPrice("totalPaid", order.total),
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() }
        )

        val totalDuration = System.currentTimeMillis() - totalStart
        Log.d(tag, "order ${order.id} - mapOrderDetails TOTAL took ${totalDuration}ms")

        return result
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
