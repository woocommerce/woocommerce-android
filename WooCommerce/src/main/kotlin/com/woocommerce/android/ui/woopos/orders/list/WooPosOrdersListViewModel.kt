package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private fun cancelJobs() {
        loadingJob?.cancel()
    }
}
