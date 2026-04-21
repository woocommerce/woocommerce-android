package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

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

    private var sideLoadJob: Job? = null
    private var refreshOrderJob: Job? = null

    private var cachedRefundData: CachedRefundData? = null

    init {
        if (singleOrderId != null) {
            viewModelScope.launch { loadSingleOrderDetail(singleOrderId) }
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

    fun onOrdersLoadingErrorRetryButtonClicked() {
        _state.value = WooPosOrdersState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
        if (singleOrderId != null) {
            viewModelScope.launch { loadSingleOrderDetail(singleOrderId) }
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
