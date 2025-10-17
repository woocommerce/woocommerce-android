package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
    private val getProductById: WooPosGetProductById,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosOrdersState>(
        WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
    )
    val state: StateFlow<WooPosOrdersState> = _state.asStateFlow()

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

        val updatedItems = current.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == orderId)
        }

        val selectedEntry = updatedItems.entries.first { (item, _) -> item.isSelected }

        _state.value = current.copy(
            items = updatedItems,
            selectedDetails = selectedEntry.value
        )
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
            childrenToParentEventSender.sendToParent(
                ToEmailReceipt(orderId)
            )
        }
    }

    fun onOrdersEmptyActionClicked() {
        // Action to be defined
    }

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosOrdersState.Loading(searchInputState = WooPosSearchInputState.Closed)
        loadOrders()
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
                appendOrders(result.getOrThrow())
            } else {
                _state.value = newState.copy(paginationState = WooPosPaginationState.Error)
            }
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

                if (event.query.isEmpty()) {
                    loadOrders()
                } else {
                    performSearch(event.query)
                }
            }

            is WooPosSearchUIEvent.Clear -> {
                updateSearchState(
                    WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query("", 0),
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

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        _state.value = when (val currentState = _state.value) {
            is WooPosOrdersState.Content -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Empty -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Error -> currentState.copy(searchInputState = searchState)
            is WooPosOrdersState.Loading -> currentState.copy(searchInputState = searchState)
        }
    }

    private fun performSearch(query: String) {
        cancelJobs()

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_DELAY_MS)
            _state.value = WooPosOrdersState.Loading(searchInputState = _state.value.searchInputState)
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
                        replaceOrders(result.orders)
                    }
                }
            }
        }
    }

    private fun loadOrders() {
        cancelJobs()
        loadingJob = viewModelScope.launch {
            ordersDataSource.loadOrders().collect { result ->
                when (result) {
                    is LoadOrdersResult.Error -> {
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
            items = items,
            selectedDetails = selectedEntry.value,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            paginationState = paginationState,
            searchInputState = _state.value.searchInputState
        )
    }

    private suspend fun appendOrders(
        orders: List<Order>,
        paginationState: WooPosPaginationState = WooPosPaginationState.None
    ) {
        val current = _state.value as WooPosOrdersState.Content
        val currentSelectedId = current.items.entries.firstOrNull { it.key.isSelected }?.key?.id
        val newItems = buildItemsMap(orders, currentSelectedId)
        val items = current.items + newItems

        _state.value = WooPosOrdersState.Content(
            items = items,
            selectedDetails = current.selectedDetails,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
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

    private fun mapOrderItem(order: Order, selectedId: Long?): OrderItemViewState {
        val formattedOrderTotals = wooCommerceStore.formatCurrencyForDisplay(
            amount = order.total.toDouble(),
            site = selectedSite.get(),
            currencyCode = null,
            applyDecimalFormatting = true
        )

        val statusText = order.status.localizedLabel(resourceProvider, locale)

        return OrderItemViewState(
            id = order.id,
            title = "#${order.number}",
            date = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            total = formattedOrderTotals,
            customerEmail = order.customer?.email,
            isSelected = order.id == selectedId,
            status = PosOrderStatus(
                text = statusText,
                colorKey = OrderStatusColorKey.fromStatus(order.status)
            )
        )
    }

    private suspend fun mapOrderDetails(order: Order): OrderDetailsViewState {
        fun fmt(amount: BigDecimal) = wooCommerceStore.formatCurrencyForDisplay(
            amount = amount.toDouble(),
            site = selectedSite.get(),
            currencyCode = null,
            applyDecimalFormatting = true
        )

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
                qtyAndUnitPrice = "${item.quantity.toInt()} x ${fmt(unitPrice)}",
                lineTotal = fmt(item.total),
                imageUrl = product?.firstImageUrl
            )
        }

        val discountCode = order.couponLines.firstOrNull()?.code
        val breakdown = OrderDetailsViewState.TotalsBreakdown(
            products = fmt(order.productsTotal),
            discount = order.discountTotal.takeIf { it != BigDecimal.ZERO }?.let { "-${fmt(it)}" },
            discountCode = discountCode,
            taxes = fmt(order.totalTax),
            shipping = order.shippingTotal.takeIf { it != BigDecimal.ZERO }?.let { fmt(it) }
        )

        return OrderDetailsViewState(
            id = order.id,
            number = "#${order.number}",
            dateTime = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            customerEmail = order.customer?.email,
            status = status,
            lineItems = lineItems,
            breakdown = breakdown,
            total = fmt(order.total),
            totalPaid = fmt(order.total),
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
