package com.woocommerce.android.ui.woopos.orders.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersCoordinator
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.BookingInfo
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundRowData
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosOrderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderDetailsMapper: WooPosOrderDetailsMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val bookingInfoMapper: WooPosBookingInfoMapper,
    private val formatPrice: WooPosFormatPrice,
    private val coordinator: WooPosOrdersCoordinator,
) : ViewModel() {

    private val singleOrderId: Long? = savedStateHandle.get<Long>(ORDERS_ROUTE_ORDER_ID_KEY)

    val isSingleOrderMode: Boolean = singleOrderId != null

    private val _state = MutableStateFlow<WooPosOrderDetailsState>(
        if (singleOrderId != null) WooPosOrderDetailsState.Loading else WooPosOrderDetailsState.Idle
    )
    val state: StateFlow<WooPosOrderDetailsState> = _state.asStateFlow()

    private var loadOrderJob: Job? = null
    private var sideLoadJob: Job? = null
    private var refreshOrderJob: Job? = null
    private var cachedRefundData: CachedRefundData? = null
    private var lastRequestedOrderId: Long? = null

    init {
        if (singleOrderId != null) {
            loadOrder(singleOrderId)
        } else {
            observeSelectedOrder()
        }
    }

    private fun observeSelectedOrder() {
        viewModelScope.launch {
            coordinator.selectedOrderId.collectLatest { orderId ->
                if (orderId == null) {
                    clearSelection()
                } else {
                    loadOrder(orderId)
                }
            }
        }
    }

    private fun clearSelection() {
        sideLoadJob?.cancel()
        refreshOrderJob?.cancel()
        cachedRefundData = null
        lastRequestedOrderId = null
        _state.value = WooPosOrderDetailsState.Idle
    }

    fun retryLoadOrder() {
        val orderId = lastRequestedOrderId ?: return
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: Long) {
        lastRequestedOrderId = orderId
        loadOrderJob?.cancel()
        sideLoadJob?.cancel()
        refreshOrderJob?.cancel()

        loadOrderJob = viewModelScope.launch {
            _state.value = WooPosOrderDetailsState.Loading

            val order = ordersDataSource.getOrderById(orderId).getOrElse {
                _state.value = WooPosOrderDetailsState.Error(
                    message = resourceProvider.getString(R.string.woopos_orders_loading_error_message)
                )
                return@launch
            }

            val details = orderDetailsMapper.mapOrderDetailsWithoutRefunds(order)
            _state.value = WooPosOrderDetailsState.Loaded(
                details = details,
                dialogState = WooPosOrderDetailsState.DialogState.Hidden
            )

            ordersAnalyticsTracker.trackOrderDetailsLoaded(
                orderId = orderId,
                orderStatus = order.status.value,
                createdAtMillis = order.dateCreated.time
            )

            sideLoadActionsAndRefundData(orderId, order)
        }
    }

    fun onEmailReceiptClicked(orderId: Long) {
        viewModelScope.launch {
            ordersAnalyticsTracker.trackOrderDetailsEmailReceiptTapped()
            childrenToParentEventSender.sendToParent(ToEmailReceipt(orderId))
        }
    }

    fun onBackFromIssueRefund(orderId: Long? = null) {
        refreshOrderAfterIssueRefund(orderId)
    }

    fun onRefundDetailsDialogDismissed() {
        val current = _state.value as? WooPosOrderDetailsState.Loaded ?: return
        _state.value = current.copy(
            dialogState = WooPosOrderDetailsState.DialogState.Hidden
        )
    }

    fun onBackFromSuccessfullySendingEmailReceipt() {
        refreshSelectedOrder()
    }

    fun onViewRefundDetailsClicked(refundIndex: Int) {
        val cached = cachedRefundData ?: return
        val rowData = cached.rows.getOrNull(refundIndex) ?: return
        val order = cached.order
        if (_state.value !is WooPosOrderDetailsState.Loaded) return

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

            val updatedState = _state.value as? WooPosOrderDetailsState.Loaded ?: return@launch
            if (updatedState.details.id != cached.orderId) return@launch
            _state.value = updatedState.copy(
                dialogState = WooPosOrderDetailsState.DialogState.RefundDetails(
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

    private fun sideLoadActionsAndRefundData(orderId: Long, order: Order) {
        sideLoadJob?.cancel()
        sideLoadJob = viewModelScope.launch {
            val refundsResult = retrieveOrderRefunds(order = order, forceRefresh = true).fold(
                onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
                onFailure = { RefundsFetchResult.Error }
            )

            val currentLoaded = _state.value as? WooPosOrderDetailsState.Loaded
            if (currentLoaded?.details?.id != orderId) return@launch

            val refundInfo = refundInfoBuilder.buildRefundInfo(order, refundsResult)
            val updatedBreakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
            val refundedLineItems = orderDetailsMapper.buildRefundedLineItems(order, refundsResult)
            val nonRefundedLineItems = orderDetailsMapper.buildNonRefundedLineItems(order, refundsResult)

            cacheRefundData(orderId, refundInfo.refundRows, order)

            val stateNow = _state.value as? WooPosOrderDetailsState.Loaded ?: return@launch
            if (stateNow.details.id == orderId) {
                _state.value = stateNow.copy(
                    details = stateNow.details.copy(
                        breakdown = updatedBreakdown,
                        lineItems = LineItemsState.Loaded(nonRefundedLineItems),
                        refundedLineItems = LineItemsState.Loaded(refundedLineItems)
                    )
                )

                val updatedLoaded = _state.value as? WooPosOrderDetailsState.Loaded
                if (updatedLoaded != null) {
                    sideLoadBookings(orderId, updatedLoaded.details)
                }
            }
        }
    }

    private fun sideLoadBookings(
        orderId: Long,
        details: Details
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

            val currentLoaded = _state.value as? WooPosOrderDetailsState.Loaded ?: return@launch
            if (currentLoaded.details.id != orderId) return@launch

            val currentItems = (currentLoaded.details.lineItems as? LineItemsState.Loaded)?.items
                ?: return@launch
            _state.value = currentLoaded.copy(
                details = currentLoaded.details.copy(
                    lineItems = LineItemsState.Loaded(
                        currentItems.map { lineItem ->
                            results[lineItem.id]?.let { lineItem.copy(bookingInfo = it) } ?: lineItem
                        }
                    )
                )
            )
        }
    }

    private fun refreshSelectedOrder() {
        refreshOrderAfterIssueRefund(orderId = null)
    }

    private fun refreshOrderAfterIssueRefund(orderId: Long?) {
        val current = _state.value as? WooPosOrderDetailsState.Loaded
        val selectedOrderId = orderId ?: current?.details?.id ?: return

        if (current?.details?.id == selectedOrderId) {
            sideLoadJob?.cancel()
        }
        refreshOrderJob?.cancel()
        refreshOrderJob = viewModelScope.launch {
            // Fetch + notify run atomically so the list row is always refreshed when the cache
            // is updated, even if the user has already selected a different order. Only the
            // detail-pane work below is cancellable and skipped on selection change.
            val updated = withContext(NonCancellable) {
                ordersDataSource.refreshOrderById(selectedOrderId).getOrNull()?.also {
                    coordinator.notifyOrderRefreshed(it.id)
                }
            } ?: return@launch
            if ((_state.value as? WooPosOrderDetailsState.Loaded)?.details?.id == updated.id) {
                applyOrderUpdate(updated)
            }
        }
    }

    private suspend fun applyOrderUpdate(updated: Order) {
        val historicalRefundsResult = retrieveOrderRefunds(updated, forceRefresh = true).fold(
            onSuccess = { refunds -> RefundsFetchResult.Success(refunds) },
            onFailure = { RefundsFetchResult.Error }
        )

        val refundInfo = refundInfoBuilder.buildRefundInfo(updated, historicalRefundsResult)
        cacheRefundData(updated.id, refundInfo.refundRows, updated)

        val newDetailsViewState = orderDetailsMapper.mapOrderDetails(updated, historicalRefundsResult)

        val current = _state.value as? WooPosOrderDetailsState.Loaded ?: return
        if (current.details.id == updated.id) {
            _state.value = current.copy(details = newDetailsViewState)
        }
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
