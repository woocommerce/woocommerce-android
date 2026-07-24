package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Suppress("LongParameterList")
@Composable
internal fun OrderListRoute(
    viewModel: OrderListViewModel,
    communicationViewModel: OrdersCommunicationViewModel,
    currencyFormatter: CurrencyFormatter,
    detailHighlightedOrderId: Long?,
    scrollToTopRequests: Flow<Unit>,
    onOrderTapped: (OrderListNavigationTarget) -> Unit,
    onRefresh: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onListAtTopChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pagedList by viewModel.pagedListData.observeAsState()
    val emptyViewType by viewModel.emptyViewType.observeAsState(EmptyViewType.ORDER_LIST_LOADING)
    val isFetchingFirstPage by viewModel.isFetchingFirstPage.observeAsState(false)
    val isLoadingMore by viewModel.isLoadingMore.observeAsState(false)
    val orderStatusOptions by viewModel.orderStatusOptions.observeAsState(emptyMap())
    val viewState by viewModel.viewStateLiveData.liveData.observeAsState(OrderListViewModel.ViewState())
    val selectedOrderIds by viewModel.selectedOrderIds.collectAsStateWithLifecycle()
    val createdOrderId by communicationViewModel.createdOrderIdPendingScrollToTopFlow
        .collectAsStateWithLifecycle()
    val presenter = remember { OrderListPaging2Presenter() }
    val presentation by presenter.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    DisposableEffect(presenter) {
        onDispose(presenter::close)
    }
    LaunchedEffect(presenter, pagedList) {
        presenter.submit(pagedList)
    }
    LaunchedEffect(presenter, viewModel) {
        viewModel.orderListContentRevision.drop(1).collect {
            presenter.markContentChanged()
        }
    }
    LaunchedEffect(scrollToTopRequests, listState) {
        scrollToTopRequests.collectLatest {
            if (listState.layoutInfo.totalItemsCount > 0) {
                listState.animateScrollToItem(0)
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect(onListAtTopChanged)
    }

    var pendingCreatedOrderId by remember { mutableStateOf<Long?>(null) }
    val currentCreatedOrderHandled = rememberUpdatedState(
        communicationViewModel::onScrollToTopAfterOrderCreationHandled
    )
    LaunchedEffect(createdOrderId) {
        pendingCreatedOrderId = createdOrderId
    }
    LaunchedEffect(
        pendingCreatedOrderId,
        presentation.generation,
        presentation.contentRevision,
    ) {
        val pendingOrderId = pendingCreatedOrderId ?: return@LaunchedEffect
        if (presentation.itemCount > 0) {
            listState.scrollToItem(0)
        }
        if (presenter.indexOfOrder(presentation, pendingOrderId) != null) {
            pendingCreatedOrderId = null
            currentCreatedOrderHandled.value()
        }
    }
    val userScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (
                    source == NestedScrollSource.UserInput &&
                    available.y != 0f &&
                    pendingCreatedOrderId != null
                ) {
                    pendingCreatedOrderId = null
                    currentCreatedOrderHandled.value()
                }
                return Offset.Zero
            }
        }
    }

    val itemKey = remember(presenter, presentation) {
        fun keyAt(index: Int): Any = checkNotNull(presenter.keyAt(presentation, index))
        ::keyAt
    }
    val itemContentType = remember(presenter, presentation) {
        fun contentTypeAt(index: Int): Any? = presenter.contentTypeAt(presentation, index)
        ::contentTypeAt
    }
    val itemAt = remember(
        presenter,
        presentation,
        orderStatusOptions,
        currencyFormatter,
        context,
    ) {
        fun itemAt(index: Int): OrderListItemUiModel? {
            return presenter.itemAt(presentation, index)?.toUiModel(
                orderStatusOptions = orderStatusOptions,
                formatCurrency = { rawValue, currencyCode ->
                    currencyFormatter.formatCurrency(rawValue, currencyCode)
                },
                resolveString = context::getString,
            )
        }
        ::itemAt
    }
    val contentState = emptyViewType.toOrderListContentState(
        query = viewModel.searchQuery,
        isAppending = isLoadingMore,
        contentRevision = presentation.contentRevision,
    )

    WCPullToRefreshBox(
        isRefreshing = isFetchingFirstPage || viewState.isBulkUpdating,
        onRefresh = onRefresh,
        enabled = selectedOrderIds.isEmpty() && !viewState.isBulkUpdating,
        modifier = modifier.fillMaxSize(),
    ) {
        OrderListContent(
            state = contentState,
            rowState = OrderListRowState(
                bulkSelectedOrderIds = selectedOrderIds,
                detailHighlightedOrderId = detailHighlightedOrderId,
            ),
            itemCount = presentation.itemCount,
            itemKey = itemKey,
            itemAt = itemAt,
            itemContentType = itemContentType,
            onOrderTapped = { orderId ->
                presenter.navigationTarget(presentation, orderId)?.let(onOrderTapped)
            },
            onOrderLongPressed = { orderId ->
                viewModel.onOrderLongPressed(orderId)
            },
            onOrderSelectionToggled = viewModel::toggleOrderSelection,
            onMarkOrderCompleted = viewModel::onSwipeToComplete,
            onLearnMoreClicked = onLearnMoreClicked,
            onShowGuestOrdersClicked = viewModel::onSearchGuestOrdersClicked,
            onRetryClicked = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(userScrollConnection),
            listState = listState,
        )
    }
}

internal data class OrderListNavigationTarget(
    val orderId: Long,
    val loadedOrderIds: List<Long>,
    val status: String,
)

internal fun EmptyViewType?.toOrderListContentState(
    query: String,
    isAppending: Boolean,
    contentRevision: Long,
): OrderListContentState = when (this) {
    EmptyViewType.ORDER_LIST_LOADING -> OrderListContentState.InitialLoading
    EmptyViewType.ORDER_LIST -> OrderListContentState.Empty(OrderListEmptyState.NoOrders)
    EmptyViewType.ORDER_LIST_FILTERED -> OrderListContentState.Empty(OrderListEmptyState.Filtered)
    EmptyViewType.SEARCH_RESULTS -> OrderListContentState.Empty(OrderListEmptyState.Search(query))
    EmptyViewType.SEARCH_RESULTS_GUEST -> OrderListContentState.Empty(OrderListEmptyState.GuestSearch(query))
    EmptyViewType.NETWORK_OFFLINE -> OrderListContentState.Empty(OrderListEmptyState.Offline)
    EmptyViewType.NETWORK_ERROR -> OrderListContentState.Empty(OrderListEmptyState.NetworkError)
    else -> OrderListContentState.Content(
        isAppending = isAppending,
        contentRevision = contentRevision,
    )
}

private fun OrderListPaging2Presenter.navigationTarget(
    presentation: OrderListPaging2Presenter.State,
    orderId: Long,
): OrderListNavigationTarget? {
    val index = indexOfOrder(presentation, orderId) ?: return null
    val order = itemAt(presentation, index) as? OrderListItemUI ?: return null
    return OrderListNavigationTarget(
        orderId = order.orderId,
        loadedOrderIds = loadedOrderIds(presentation),
        status = order.status,
    )
}
