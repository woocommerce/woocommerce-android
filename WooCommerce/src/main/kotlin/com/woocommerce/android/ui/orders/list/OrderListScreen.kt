@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.orders.list

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.AngleDown
import com.woocommerce.android.ui.compose.designsystem.icons.AngleUp
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.Plus
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.jitm.JitmState
import com.woocommerce.android.ui.jitm.JitmViewModel
import com.woocommerce.android.ui.orders.OrdersCommunicationViewModel
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun OrderListScreen(
    viewModel: OrderListViewModel,
    communicationViewModel: OrdersCommunicationViewModel,
    currencyFormatter: CurrencyFormatter,
    detailHighlightedOrderId: Long?,
    scrollToTopRequests: Flow<Unit>,
    jitmViewModelProvider: () -> JitmViewModel,
    onOrderTapped: (OrderListNavigationTarget) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onCreateOrderClicked: () -> Unit,
    onTroubleshootingClicked: (OrderListTroubleshootingType) -> Unit,
    onContactSupportClicked: (OrderListTroubleshootingType) -> Unit,
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
    val lastUpdate by viewModel.lastUpdateOrdersList.observeAsState()
    val selectedOrderIds by viewModel.selectedOrderIds.collectAsStateWithLifecycle()
    val createdOrderId by communicationViewModel.createdOrderIdPendingScrollToTopFlow
        .collectAsStateWithLifecycle()
    val presenter = remember { OrderListPaging2Presenter() }
    // Deferred lazy-list lambdas must share this exact count, snapshot, and generation.
    val presentation = presenter.state.collectAsStateWithLifecycle().value
    val listState = rememberLazyListState()
    var programmaticScrollCount by remember { mutableIntStateOf(0) }
    var pendingCreatedOrderId by remember { mutableStateOf<Long?>(null) }
    val currentCreatedOrderHandled = rememberUpdatedState(
        communicationViewModel::onScrollToTopAfterOrderCreationHandled
    )

    suspend fun runProgrammaticScroll(scroll: suspend () -> Unit) {
        programmaticScrollCount += 1
        try {
            scroll()
        } finally {
            programmaticScrollCount -= 1
        }
    }

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
                runProgrammaticScroll {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect(onListAtTopChanged)
    }

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
            runProgrammaticScroll {
                listState.scrollToItem(0)
            }
        }
        if (presenter.indexOfOrder(presentation, pendingOrderId) != null) {
            pendingCreatedOrderId = null
            currentCreatedOrderHandled.value()
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to programmaticScrollCount
        }.collect { (isScrollInProgress, activeProgrammaticScrolls) ->
            if (
                isScrollInProgress &&
                activeProgrammaticScrolls == 0 &&
                pendingCreatedOrderId != null
            ) {
                pendingCreatedOrderId = null
                currentCreatedOrderHandled.value()
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
        query = viewState.searchQuery,
        isAppending = isLoadingMore,
        contentRevision = presentation.contentRevision,
    )
    val troubleshootingType = viewState.toTroubleshootingType()
    var isTroubleshootingExpanded by rememberSaveable(troubleshootingType) {
        mutableStateOf(true)
    }
    val screenState = OrderListScreenState(
        isSearchActive = viewState.isSearching,
        searchQuery = viewState.searchQuery,
        filterCount = viewState.filterCount,
        lastUpdate = lastUpdate,
        rowState = OrderListRowState(
            bulkSelectedOrderIds = selectedOrderIds,
            detailHighlightedOrderId = detailHighlightedOrderId,
        ),
        troubleshooting = troubleshootingType?.let {
            OrderListTroubleshootingPresentation(
                type = it,
                isExpanded = isTroubleshootingExpanded,
            )
        },
    )
    val jitmViewModel = if (viewState.jitmEnabled) jitmViewModelProvider() else null
    val jitmState = if (jitmViewModel != null) {
        val state by jitmViewModel.jitmState.observeAsState()
        state
    } else {
        null
    }

    OrderListScreen(
        state = screenState,
        orderListContent = { contentModifier ->
            WCPullToRefreshBox(
                isRefreshing = isFetchingFirstPage || viewState.isBulkUpdating,
                onRefresh = viewModel::onPullToRefresh,
                enabled = selectedOrderIds.isEmpty() && !viewState.isBulkUpdating,
                modifier = contentModifier,
            ) {
                OrderListContent(
                    state = contentState,
                    rowState = screenState.rowState,
                    itemCount = presentation.itemCount,
                    itemKey = itemKey,
                    itemAt = itemAt,
                    itemContentType = itemContentType,
                    onOrderActivated = { orderId ->
                        if (
                            viewModel.onOrderActivated(orderId) ==
                            OrderListViewModel.OrderActivation.OPEN_DETAIL
                        ) {
                            presenter.navigationTarget(presentation, orderId)?.let(onOrderTapped)
                        }
                    },
                    onOrderLongPressed = viewModel::onOrderLongPressed,
                    onOrderSelectionToggled = viewModel::toggleOrderSelection,
                    onMarkOrderCompleted = viewModel::onSwipeToComplete,
                    onLearnMoreClicked = onLearnMoreClicked,
                    onShowGuestOrdersClicked = viewModel::onSearchGuestOrdersClicked,
                    onRetryClicked = viewModel::fetchOrdersAndOrderDependencies,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState,
                )
            }
        },
        onSearchClicked = viewModel::onSearchOpened,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSearchSubmitted = viewModel::onSearchSubmitted,
        onSearchClosed = viewModel::onSearchClosed,
        onBarcodeClicked = viewModel::onScanClicked,
        onFiltersClicked = viewModel::onFiltersButtonTapped,
        onCreateOrderClicked = onCreateOrderClicked,
        onSelectionCloseClicked = viewModel::clearOrderSelection,
        onSelectionUpdateStatusClicked = viewModel::onBulkUpdateStatusClicked,
        onTroubleshootingExpandedChanged = { isTroubleshootingExpanded = it },
        onTroubleshootingClicked = {
            troubleshootingType?.let(onTroubleshootingClicked)
        },
        onContactSupportClicked = {
            troubleshootingType?.let(onContactSupportClicked)
        },
        modifier = modifier.fillMaxSize(),
        jitmContent = when (jitmState) {
            is JitmState.Banner, is JitmState.Modal -> {
                { OrderListJitm(jitmState) }
            }
            JitmState.Hidden, null -> null
        },
    )
}

@Composable
@Suppress("LongParameterList")
internal fun OrderListScreen(
    state: OrderListScreenState,
    orderListContent: @Composable (Modifier) -> Unit,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onBarcodeClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
    onCreateOrderClicked: () -> Unit,
    onSelectionCloseClicked: () -> Unit,
    onSelectionUpdateStatusClicked: () -> Unit,
    onTroubleshootingExpandedChanged: (Boolean) -> Unit,
    onTroubleshootingClicked: () -> Unit,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier,
    jitmContent: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(OrderListTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OrderListHeader(
                content = state.headerContent,
                searchQuery = state.searchQuery,
                filterCount = state.filterCount,
                lastUpdate = state.lastUpdate,
                onSearchClicked = onSearchClicked,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchSubmitted = onSearchSubmitted,
                onSearchClosed = onSearchClosed,
                onBarcodeClicked = onBarcodeClicked,
                onFiltersClicked = onFiltersClicked,
                onSelectionCloseClicked = onSelectionCloseClicked,
                onSelectionUpdateStatusClicked = onSelectionUpdateStatusClicked,
            )

            state.troubleshooting?.let { presentation ->
                OrderListTroubleshooting(
                    presentation = presentation,
                    onExpandedChanged = onTroubleshootingExpandedChanged,
                    onTroubleshootingClicked = onTroubleshootingClicked,
                    onContactSupportClicked = onContactSupportClicked,
                )
            }

            jitmContent?.let { content ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OrderListTestTags.JITM),
                ) {
                    content()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(OrderListTestTags.BODY),
            ) {
                orderListContent(Modifier.fillMaxSize())
            }
        }

        AnimatedVisibility(
            visible = state.shouldShowCreateOrderFab,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WooTheme.padding.padding5),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            FloatingActionButton(
                onClick = onCreateOrderClicked,
                containerColor = WooTheme.colors.primary,
                contentColor = WooTheme.colors.onPrimary,
                modifier = Modifier.testTag(OrderListTestTags.CREATE_ORDER_FAB),
            ) {
                Icon(
                    imageVector = WooIcons.Regular.Plus,
                    contentDescription = stringResource(R.string.orderlist_create_order_button_description),
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderListTroubleshooting(
    presentation: OrderListTroubleshootingPresentation,
    onExpandedChanged: (Boolean) -> Unit,
    onTroubleshootingClicked: () -> Unit,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(OrderListTestTags.TROUBLESHOOTING),
    ) {
        Surface(
            color = WooTheme.colors.surface.default,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = presentation.isExpanded,
                            role = Role.Button,
                            onValueChange = onExpandedChanged,
                        )
                        .testTag(OrderListTestTags.TROUBLESHOOTING_TOGGLE)
                        .padding(
                            horizontal = WooTheme.padding.padding7,
                            vertical = WooTheme.padding.padding4,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = WooIcons.Regular.CircleInfo,
                        contentDescription = null,
                        tint = WooTheme.colors.primary,
                        modifier = Modifier.size(WooTheme.iconSize.size24),
                    )
                    Spacer(modifier = Modifier.width(WooTheme.spacing.space4))
                    Text(
                        text = stringResource(presentation.type.titleRes),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                        color = WooTheme.colors.surface.onDefault,
                        style = WooTheme.text.titleMedium.strong,
                    )
                    Spacer(modifier = Modifier.width(WooTheme.spacing.space3))
                    Icon(
                        imageVector = if (presentation.isExpanded) {
                            WooIcons.Regular.AngleUp
                        } else {
                            WooIcons.Regular.AngleDown
                        },
                        contentDescription = null,
                        tint = WooTheme.colors.surface.onVariant,
                        modifier = Modifier.size(WooTheme.iconSize.size18),
                    )
                }
                AnimatedVisibility(visible = presentation.isExpanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = WooTheme.padding.padding7,
                            end = WooTheme.padding.padding7,
                            bottom = WooTheme.padding.padding5,
                        ),
                        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
                    ) {
                        Text(
                            text = stringResource(presentation.type.messageRes),
                            color = WooTheme.colors.surface.onVariant,
                            style = WooTheme.text.bodyMedium.regular,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                WooTheme.spacing.space3,
                                Alignment.End,
                            ),
                            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                        ) {
                            WooOutlinedButton(
                                text = stringResource(R.string.error_troubleshooting),
                                onClick = onTroubleshootingClicked,
                                size = WooButtonSize.Small,
                                modifier = Modifier.testTag(
                                    OrderListTestTags.TROUBLESHOOTING_ACTION
                                ),
                            )
                            WooOutlinedButton(
                                text = stringResource(R.string.support_contact),
                                onClick = onContactSupportClicked,
                                size = WooButtonSize.Small,
                                modifier = Modifier.testTag(
                                    OrderListTestTags.CONTACT_SUPPORT_ACTION
                                ),
                            )
                        }
                    }
                }
            }
        }
        WooDivider()
    }
}

@get:StringRes
private val OrderListTroubleshootingType.titleRes: Int
    get() = when (this) {
        OrderListTroubleshootingType.ParsingError -> R.string.orderlist_parsing_error_title
        OrderListTroubleshootingType.Timeout -> R.string.orderlist_timeout_error_title
    }

@get:StringRes
private val OrderListTroubleshootingType.messageRes: Int
    get() = when (this) {
        OrderListTroubleshootingType.ParsingError -> R.string.orderlist_parsing_error_message
        OrderListTroubleshootingType.Timeout -> R.string.orderlist_timeout_error_message
    }

private fun OrderListViewModel.ViewState.toTroubleshootingType(): OrderListTroubleshootingType? {
    return when {
        isErrorFetchingDataBannerVisible -> OrderListTroubleshootingType.ParsingError
        shouldDisplayTroubleshootingBanner -> OrderListTroubleshootingType.Timeout
        else -> null
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

@PreviewLightDark
@Composable
private fun OrderListBrowsingPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
    )
}

@Preview(name = "Filtered with JITM", heightDp = 780)
@Composable
private fun OrderListFilteredWithJitmPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            filterCount = 3,
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
        jitmContent = {
            WooNoticeBanner(
                title = "Grow your business with Woo",
                description = "A just-in-time message supplied by the Orders host.",
                tone = WooNoticeBannerTone.Info,
                modifier = Modifier.padding(
                    horizontal = WooTheme.padding.padding5,
                    vertical = WooTheme.padding.padding3,
                ),
            )
        },
    )
}

@Preview(name = "Search", heightDp = 700)
@Composable
private fun OrderListSearchPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            isSearchActive = true,
            searchQuery = "#1002",
            filterCount = 2,
        ),
        items = previewOrderListItems.takeLast(1),
    )
}

@Preview(name = "Selection over search", heightDp = 700)
@Composable
private fun OrderListSelectionPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            isSearchActive = true,
            searchQuery = "Ada",
            rowState = OrderListRowState(
                bulkSelectedOrderIds = setOf(1L),
                detailHighlightedOrderId = 2L,
            ),
        ),
    )
}

@PreviewLightDark
@Composable
private fun OrderListTroubleshootingPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            troubleshooting = OrderListTroubleshootingPresentation(
                type = OrderListTroubleshootingType.Timeout,
            ),
        ),
    )
}

@Preview(name = "Troubleshooting collapsed", heightDp = 700)
@Composable
private fun OrderListTroubleshootingCollapsedPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            troubleshooting = OrderListTroubleshootingPresentation(
                type = OrderListTroubleshootingType.ParsingError,
                isExpanded = false,
            ),
        ),
    )
}

@Preview(name = "Narrow large font", widthDp = 320, heightDp = 700, fontScale = 2f)
@Composable
private fun OrderListLargeFontPreview() {
    OrderListScreenPreview(
        state = OrderListScreenState(
            filterCount = 12,
            lastUpdate = "Last updated Jul 28, 10:42 AM",
        ),
    )
}

@Composable
private fun OrderListScreenPreview(
    state: OrderListScreenState,
    items: List<OrderListItemUiModel?> = previewOrderListItems,
    jitmContent: (@Composable () -> Unit)? = null,
) {
    WooDesignSystemThemeWithBackground {
        OrderListScreen(
            state = state,
            orderListContent = { modifier ->
                OrderListContent(
                    state = OrderListContentState.Content(),
                    rowState = state.rowState,
                    itemCount = items.size,
                    itemKey = { index -> previewOrderListItemKey(index, items[index]) },
                    itemAt = items::get,
                    onOrderActivated = {},
                    onOrderLongPressed = {},
                    onOrderSelectionToggled = { true },
                    onMarkOrderCompleted = {},
                    onLearnMoreClicked = {},
                    onShowGuestOrdersClicked = {},
                    onRetryClicked = {},
                    modifier = modifier,
                )
            },
            onSearchClicked = {},
            onSearchQueryChanged = {},
            onSearchSubmitted = {},
            onSearchClosed = {},
            onBarcodeClicked = {},
            onFiltersClicked = {},
            onCreateOrderClicked = {},
            onSelectionCloseClicked = {},
            onSelectionUpdateStatusClicked = {},
            onTroubleshootingExpandedChanged = {},
            onTroubleshootingClicked = {},
            onContactSupportClicked = {},
            jitmContent = jitmContent,
        )
    }
}

private fun previewOrderListItemKey(
    index: Int,
    item: OrderListItemUiModel?,
): Any = when (item) {
    is OrderListItemUiModel.DateSection -> "section-${item.title}"
    is OrderListItemUiModel.Order -> "order-${item.orderId}"
    is OrderListItemUiModel.Loading -> "loading-${item.orderId}"
    null -> "placeholder-$index"
}

private val previewOrderListItems = listOf(
    OrderListItemUiModel.DateSection("Today"),
    previewOrderListOrder(
        orderId = 1L,
        number = "#1001",
        customerName = "Ada Lovelace",
        total = "\$48.00",
    ),
    previewOrderListOrder(
        orderId = 2L,
        number = "#1002",
        customerName = "Grace Hopper",
        total = "\$86.50",
        showDivider = false,
    ),
)

private fun previewOrderListOrder(
    orderId: Long,
    number: String,
    customerName: String,
    total: String,
    showDivider: Boolean = true,
) = OrderListItemUiModel.Order(
    orderId = orderId,
    number = number,
    customerName = customerName,
    dateCreated = "Jul 28, 2026 10:30",
    total = total,
    badges = listOf(
        OrderListBadgeUiModel(
            text = "Processing",
            containerColorRes = R.color.tag_bg_processing,
            contentColorRes = R.color.tagView_text,
        ),
        OrderListBadgeUiModel(
            text = "POS",
            containerColorRes = R.color.tag_bg_pos,
            contentColorRes = R.color.tag_text_pos,
        ),
    ),
    showDivider = showDivider,
)
