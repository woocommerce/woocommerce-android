package com.woocommerce.android.ui.woopos.orders

import android.os.SystemClock
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
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListRowTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListSearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrderDetailsLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrderDetailsEmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListSearchResultsFetched
import org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
    private val getProductById: WooPosGetProductById,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val formatPrice: WooPosFormatPrice,
    private val getOrderRefunds: WooPosGetOrderRefundsByOrderId,
    private val analyticsTracker: WooPosAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(
        WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
    )
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()
    private data class OrderTrackingMeta(
        val createdAtMillis: Long,
        val statusSlug: String
    )
    private val trackingMeta = mutableMapOf<Long, OrderTrackingMeta>()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var loadingJob: Job? = null
    private var loadingMoreOrdersJob: Job? = null

    private var ordersLoadStartMs: Long? = null
    private var searchStartMs: Long? = null

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

        trackOrdersListRowTapped(orderId, position)

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

    fun onOrdersDetailsShown(orderId: Long) {
        trackOrderDetailsShown(orderId)
    }

    private inline fun retrieveOrderTrackingData(
        orderId: Long,
        crossinline block: (meta: OrderTrackingMeta, daysSince: Int) -> Unit
    ) {
        val meta = trackingMeta[orderId] ?: return
        val daysSince = (((System.currentTimeMillis() - meta.createdAtMillis) / MILLIS_PER_DAY)
            .toInt()
            .coerceAtLeast(0))
        viewModelScope.launch { block(meta, daysSince) }
    }

    private fun trackOrderDetailsShown(orderId: Long) = retrieveOrderTrackingData(orderId) { meta, days ->
        viewModelScope.launch {
            analyticsTracker.track(
                OrderDetailsLoaded(
                    orderId = orderId,
                    orderStatus = meta.statusSlug,
                    daysSinceCreated = days
                )
            )
        }
    }

    private fun trackOrdersListRowTapped(orderId: Long, position: Int) = retrieveOrderTrackingData(orderId) { meta, days ->
        viewModelScope.launch {
            analyticsTracker.track(
                OrdersListRowTapped(
                    orderId = orderId,
                    orderStatus = meta.statusSlug,
                    listPosition = position,
                    daysSinceCreated = days
                )
            )
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            analyticsTracker.track(OrdersListPullToRefreshTriggered)
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
            analyticsTracker.track(OrderDetailsEmailReceiptTapped)
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
                analyticsTracker.track(OrdersListNextPageLoaded)
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
                    analyticsTracker.track(OrdersListSearchButtonTapped)
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

        cacheOrderTrackingMeta(updated)

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

            searchStartMs = SystemClock.elapsedRealtime()
            val result = ordersDataSource.searchOrders(query)
            when (result) {
                is SearchOrdersResult.Error -> {
                    searchStartMs = null
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
                    searchStartMs?.let { start ->
                        val elapsed = SystemClock.elapsedRealtime() - start
                        analyticsTracker.track(OrdersListSearchResultsFetched(elapsed))
                    }
                    searchStartMs = null

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
        ordersLoadStartMs = SystemClock.elapsedRealtime()
        loadingJob = viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
                        ordersLoadStartMs = null
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
                        ordersLoadStartMs?.let { start ->
                            val elapsed = SystemClock.elapsedRealtime() - start
                            analyticsTracker.track(OrdersListFetched(elapsed))
                        }
                        ordersLoadStartMs = null

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
        val newSelectedId = requireNotNull(orders.firstOrNull()?.id) { "Content requires at least one order" }
        val items = buildItemsMap(orders, newSelectedId)
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
        return orders.associate { order ->
            val item = mapOrderItem(order, selectedId)
            val details = mapOrderDetails(order)
            item to details
        }
    }

    private suspend fun mapOrderItem(order: Order, selectedId: Long?): OrderItemViewState {
        val statusText = order.status.localizedLabel(resourceProvider, locale)

        cacheOrderTrackingMeta(order)

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
            )
        )
    }

    private fun cacheOrderTrackingMeta(order: Order) {
        trackingMeta[order.id] = OrderTrackingMeta(
            createdAtMillis = order.dateCreated.time,
            statusSlug = order.status.toString()
        )
    }

    private suspend fun mapOrderDetails(order: Order): OrderDetailsViewState {
        val statusText = order.status.localizedLabel(resourceProvider, locale)

        val status = PosOrderStatus(
            text = statusText,
            colorKey = OrderStatusColorKey.fromStatus(order.status)
        )

        val lineItems = order.items.map { item ->
            val unitPrice = if (item.quantity == 0f) item.total else item.total / item.quantity.toBigDecimal()
            val product = getProductById(item.productId)
            OrderDetailsViewState.LineItemRow(
                id = item.itemId,
                name = item.name,
                qtyAndUnitPrice = "${item.quantity.toInt()} x ${formatPrice(unitPrice)}",
                lineTotal = formatPrice(item.total),
                imageUrl = product?.firstImageUrl
            )
        }

        val discountCode = order.couponLines.firstOrNull()?.code

        val refunds = getOrderRefunds(order.id)
        val refundAmounts = refunds.map { "-${formatPrice(it.amount)}" }
        val totalRefunded = refunds.sumOf { it.amount }
        val netPayment = if (totalRefunded > BigDecimal.ZERO) {
            formatPrice(order.total - totalRefunded)
        } else {
            null
        }

        val breakdown = OrderDetailsViewState.TotalsBreakdown(
            products = formatPrice(order.productsTotal),
            discount = order.discountTotal.takeIf { it != BigDecimal.ZERO }?.let { "-${formatPrice(it)}" },
            discountCode = discountCode,
            taxes = formatPrice(order.totalTax),
            shipping = order.shippingTotal.takeIf { it != BigDecimal.ZERO }?.let { formatPrice(it) },
            refunds = refundAmounts,
            netPayment = netPayment
        )

        return OrderDetailsViewState(
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
