package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.bookings.details.WooPosBookingDetailsMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersUIEvent
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

@Suppress("LargeClass", "MagicNumber")
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderItemMapper: WooPosOrderItemMapper,
    private val bookingDetailsMapper: WooPosBookingDetailsMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosBookingActionsProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(
        WooPosBookingsState.Loading
    )
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private var loadingJob: Job? = null
    private var loadingMoreOrdersJob: Job? = null
    private var sideLoadActionsJob: Job? = null

    init {
        loadOrders()
    }

    fun onUIEvent(event: WooPosOrdersUIEvent) {
        when (event) {
            is WooPosOrdersUIEvent.BookingActionClicked -> handleActionClicked(event.action)
        }
    }

    private fun handleActionClicked(action: WooPosBookingsState.BookingAction) {
        when (action) {
            is WooPosBookingsState.BookingAction.EmailReceipt -> onEmailReceiptButtonClicked(action.orderId)
            is WooPosBookingsState.BookingAction.IssueRefund -> onIssueRefundButtonClicked(action.orderId)
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
            current.selectedDetails.actionsState is WooPosBookingsState.BookingActionsState.Loaded
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
        orderDetailsViewState: WooPosBookingsState.BookingDetailsViewState?
    ): WooPosBookingsState.BookingDetailsViewState.Computed.Details {
        return if (orderDetailsViewState is WooPosBookingsState.BookingDetailsViewState.Lazy) {
            getOrComputeDetailsWithoutActions(orderId)
        } else {
            getOrComputeDetails(orderId)
        }
    }

    private fun updateItemsSelection(
        loadedItems: WooPosBookingsState.Content.Items.Loaded,
        orderId: Long,
        details: WooPosBookingsState.BookingDetailsViewState.Computed.Details
    ): Map<WooPosBookingsState.BookingItemViewState, WooPosBookingsState.BookingDetailsViewState> {
        return loadedItems.items.mapKeys { (item, _) ->
            item.copy(isSelected = item.id == orderId)
        }.mapValues { (item, orderDetails) ->
            if (item.id == orderId && orderDetails is WooPosBookingsState.BookingDetailsViewState.Lazy) {
                WooPosBookingsState.BookingDetailsViewState.Computed(orderId = orderId, details = details)
            } else {
                orderDetails
            }
        }
    }

    private suspend fun startSideLoadActionsIfNeeded(
        orderId: Long,
        details: WooPosBookingsState.BookingDetailsViewState.Computed.Details,
        orderDetailsViewState: WooPosBookingsState.BookingDetailsViewState?,
        loadedItems: WooPosBookingsState.Content.Items.Loaded
    ) {
        if (details.actionsState !is WooPosBookingsState.BookingActionsState.Loading) return

        val order = getOrderForSideLoading(orderId, orderDetailsViewState, loadedItems) ?: return
        sideLoadActions(orderId, order)
    }

    private suspend fun getOrderForSideLoading(
        orderId: Long,
        orderDetailsViewState: WooPosBookingsState.BookingDetailsViewState?,
        loadedItems: WooPosBookingsState.Content.Items.Loaded
    ): Order? {
        return when (orderDetailsViewState) {
            is WooPosBookingsState.BookingDetailsViewState.Lazy -> orderDetailsViewState.order
            is WooPosBookingsState.BookingDetailsViewState.Computed -> {
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
                updatedSelectedDetails.actionsState is WooPosBookingsState.BookingActionsState.Loading
            ) {
                val loadedItems = updatedState.items as? WooPosBookingsState.Content.Items.Loaded
                val updatedItems = loadedItems?.items?.map { (item, details) ->
                    val updatedDetails =
                        if (item.id == orderId && details is WooPosBookingsState.BookingDetailsViewState.Computed) {
                            WooPosBookingsState.BookingDetailsViewState.Computed(
                                orderId = orderId,
                                details = details.details.copy(
                                    actionsState = WooPosBookingsState.BookingActionsState.Loaded(actions),
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
                        actionsState = WooPosBookingsState.BookingActionsState.Loaded(actions),
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

        loadOrders(isRefreshing = true)
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
        _state.value = WooPosBookingsState.Loading
        loadOrders()
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
            val result = ordersDataSource.loadMore()

            if (result.isSuccess) {
                ordersAnalyticsTracker.trackOrdersListNextPageLoaded()
                appendOrders(result.getOrThrow())
            } else {
                _state.value = newState.copy(paginationState = WooPosPaginationState.Error)
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
        val newDetailsViewState = bookingDetailsMapper.mapBookingDetails(updated, historicalRefundsResult)
        val newDetails = WooPosBookingsState.BookingDetailsViewState.Computed(
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
                        )
                    }

                    is LoadOrdersResult.SuccessCache -> {
                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosBookingsState.Loading
                        } else {
                            replaceOrders(result.ordersWithRefunds)
                        }
                    }

                    is LoadOrdersResult.SuccessRemote -> {
                        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
                        ordersAnalyticsTracker.trackOrdersListFetched(elapsedMs)

                        if (result.ordersWithRefunds.isEmpty()) {
                            _state.value = WooPosBookingsState.Empty()
                        } else {
                            replaceOrders(result.ordersWithRefunds)
                        }
                    }
                }
            }
        }
    }

    private fun cancelJobs() {
        loadingJob?.cancel()
        loadingMoreOrdersJob?.cancel()
        sideLoadActionsJob?.cancel()
    }

    private suspend fun getOrComputeDetails(orderId: Long): WooPosBookingsState.BookingDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosBookingsState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosBookingsState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return bookingDetailsMapper.mapBookingDetails(
            when (orderDetails) {
                is WooPosBookingsState.BookingDetailsViewState.Lazy -> orderDetails.order
                is WooPosBookingsState.BookingDetailsViewState.Computed -> {
                    loadedItems.items.keys.firstOrNull { it.id == orderId }?.let {
                        ordersDataSource.getOrderById(it.id).getOrNull()
                    } ?: error("Order $orderId not found")
                }
            },
            when (orderDetails) {
                is WooPosBookingsState.BookingDetailsViewState.Lazy -> orderDetails.refundResult
                is WooPosBookingsState.BookingDetailsViewState.Computed -> RefundsFetchResult.Error
            }
        )
    }

    private suspend fun getOrComputeDetailsWithoutActions(
        orderId: Long
    ): WooPosBookingsState.BookingDetailsViewState.Computed.Details {
        val current = _state.value as? WooPosBookingsState.Content ?: error("State is not Content")
        val loadedItems = current.items as? WooPosBookingsState.Content.Items.Loaded ?: error("Items not loaded")

        val orderDetails = loadedItems.items.values.firstOrNull { it.orderId == orderId }
            ?: error("Order $orderId not found in state")

        return when (orderDetails) {
            is WooPosBookingsState.BookingDetailsViewState.Lazy ->
                bookingDetailsMapper.mapBookingDetailsWithoutActions(orderDetails.order)

            is WooPosBookingsState.BookingDetailsViewState.Computed -> {
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
            is WooPosBookingsState.BookingDetailsViewState.Computed -> details.details
            is WooPosBookingsState.BookingDetailsViewState.Lazy -> error("Selected order should have computed details")
        }

        _state.value = WooPosBookingsState.Content(
            items = WooPosBookingsState.Content.Items.Loaded(
                items = items
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = selectedDetails,
            paginationState = paginationState,
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
            dialogState = current.dialogState
        )
    }

    private suspend fun buildItemsMap(
        ordersWithRefunds: Map<Order, RefundsFetchResult>,
        selectedId: Long?
    ): Map<WooPosBookingsState.BookingItemViewState, WooPosBookingsState.BookingDetailsViewState> = coroutineScope {
        ordersWithRefunds.map { (order, refundResult) ->
            async {
                val item = orderItemMapper.mapOrderItem(order, selectedId)
                val details: WooPosBookingsState.BookingDetailsViewState = if (order.id == selectedId) {
                    val fullDetails = bookingDetailsMapper.mapBookingDetails(order, refundResult)
                    WooPosBookingsState.BookingDetailsViewState.Computed(orderId = order.id, details = fullDetails)
                } else {
                    WooPosBookingsState.BookingDetailsViewState.Lazy(
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
