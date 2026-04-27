package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.rememberRetained
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetails
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsViewModel
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosIssueRefundDialog
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundDetailsDialog
import com.woocommerce.android.ui.woopos.orders.list.WooPosOrdersListState
import com.woocommerce.android.ui.woopos.orders.list.WooPosOrdersListViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    navigatedFromEmailReceiptSent: Boolean,
    refundReasonResult: String? = null,
) {
    val isPhoneLayout = LocalContext.current.isWooPosPhoneLayout()
    val listViewModel: WooPosOrdersListViewModel = hiltViewModel()
    val detailViewModel: WooPosOrderDetailsViewModel = hiltViewModel()

    val listState by listViewModel.state.collectAsState()
    val detailState by detailViewModel.state.collectAsState()

    LaunchedEffect(navigatedFromEmailReceiptSent) {
        if (navigatedFromEmailReceiptSent) {
            detailViewModel.onBackFromSuccessfullySendingEmailReceipt()
        }
    }

    WooPosOrdersScreen(
        listState = listState,
        detailState = detailState,
        isSingleOrderMode = detailViewModel.isSingleOrderMode,
        isPhoneLayout = isPhoneLayout,
        scrollToTopEvent = listViewModel.scrollToTopEvent,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = listViewModel::onRefresh,
        onOrderSelected = if (isPhoneLayout && !detailViewModel.isSingleOrderMode) {
            { orderId -> onNavigationEvent(WooPosNavigationEvent.OpenOrderDetails(orderId)) }
        } else {
            listViewModel::onOrderSelected
        },
        onEndOfOrdersListReached = listViewModel::onEndOfOrdersListReached,
        onPaginationErrorTryAgain = listViewModel::onPaginationErrorTryAgain,
        onSearchEvent = listViewModel::onSearchEvent,
        onSearchErrorRetry = listViewModel::onSearchErrorRetry,
        onOrdersEmptyActionClicked = listViewModel::onOrdersEmptyActionClicked,
        onOrdersLoadingErrorRetryButtonClicked = listViewModel::onOrdersLoadingErrorRetryButtonClicked,
        onUIEvent = detailViewModel::onUIEvent,
        onRetryDetailLoad = detailViewModel::retryLoadOrder,
        onIssueRefundDialogDismissed = detailViewModel::onIssueRefundDialogDismissed,
        onRefundDetailsDialogDismissed = detailViewModel::onRefundDetailsDialogDismissed,
        onNavigationEvent = onNavigationEvent,
        refundReasonUpdate = refundReasonResult
    )
}

@Composable
private fun WooPosOrdersScreen(
    listState: WooPosOrdersListState,
    detailState: WooPosOrderDetailsState,
    isSingleOrderMode: Boolean = false,
    isPhoneLayout: Boolean = false,
    scrollToTopEvent: SharedFlow<Unit>,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onOrdersEmptyActionClicked: () -> Unit,
    onOrdersLoadingErrorRetryButtonClicked: () -> Unit,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    onIssueRefundDialogDismissed: () -> Unit,
    onRefundDetailsDialogDismissed: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    refundReasonUpdate: String? = null,
) {
    BackHandler { onBackClicked() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSingleOrderMode) {
            when (detailState) {
                is WooPosOrderDetailsState.Loaded -> {
                    SingleOrderDetails(detailState = detailState, onUIEvent = onUIEvent)
                }
                is WooPosOrderDetailsState.Loading -> {
                    OrderDetailsLoadingPane(
                        showOrderNumber = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(
                                start = WooPosSpacing.Medium.value,
                                end = WooPosSpacing.Medium.value,
                                bottom = WooPosSpacing.XLarge.value
                            )
                    )
                }
                is WooPosOrderDetailsState.Error -> {
                    OrdersError(
                        onRetryClicked = onRetryDetailLoad,
                        modifier = Modifier.statusBarsPadding()
                    )
                }
                is WooPosOrderDetailsState.Idle -> {
                    // Nothing to show in single order mode when idle
                }
            }
        } else {
            when (listState) {
                is WooPosOrdersListState.Content -> {
                    if (isPhoneLayout) {
                        // onUIEvent and onRetryDetailLoad are unused here — order details
                        // are shown on a separate screen via WooPosNavigationEvent.OpenOrderDetails
                        val deselectedState = remember(listState) { listState.withoutSelection() }
                        OrdersListPane(
                            state = deselectedState,
                            scrollToTopEvent = scrollToTopEvent,
                            onRefresh = onRefresh,
                            isRefreshing = listState.pullToRefreshState ==
                                WooPosPullToRefreshState.Refreshing,
                            onOrderSelected = onOrderSelected,
                            onEndOfOrdersListReached = onEndOfOrdersListReached,
                            onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                            onSearchEvent = onSearchEvent,
                            onSearchErrorRetry = onSearchErrorRetry,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        )
                    } else {
                        OrdersListWithDetails(
                            listContent = listState,
                            detailState = detailState,
                            scrollToTopEvent = scrollToTopEvent,
                            onRefresh = onRefresh,
                            onOrderSelected = onOrderSelected,
                            onEndOfOrdersListReached = onEndOfOrdersListReached,
                            onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                            onSearchEvent = onSearchEvent,
                            onSearchErrorRetry = onSearchErrorRetry,
                            onUIEvent = onUIEvent,
                            onRetryDetailLoad = onRetryDetailLoad
                        )
                    }
                }
                is WooPosOrdersListState.Empty -> OrdersEmpty(
                    onActionClicked = onOrdersEmptyActionClicked,
                    modifier = Modifier.statusBarsPadding()
                )
                is WooPosOrdersListState.Error -> OrdersError(
                    onRetryClicked = onOrdersLoadingErrorRetryButtonClicked,
                    modifier = Modifier.statusBarsPadding()
                )
                is WooPosOrdersListState.Loading -> WooPosOrdersLoadingScreen()
            }
        }

        if (listState.searchInputState is WooPosSearchInputState.Closed) {
            val toolbarTitle = if (isSingleOrderMode) {
                val orderNumber = (detailState as? WooPosOrderDetailsState.Loaded)
                    ?.details?.number.orEmpty()
                stringResource(R.string.woopos_order_title, orderNumber)
            } else {
                stringResource(R.string.woopos_orders_title)
            }
            WooPosToolbar(
                titleText = toolbarTitle,
                onBackClicked = onBackClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )
        }

        val loadedDetailState = detailState as? WooPosOrderDetailsState.Loaded
        if (loadedDetailState != null) {
            OrdersDialogs(
                dialogState = loadedDetailState.dialogState,
                onIssueRefundDialogDismissed = onIssueRefundDialogDismissed,
                onRefundDetailsDialogDismissed = onRefundDetailsDialogDismissed,
                onNavigationEvent = onNavigationEvent,
                refundReasonUpdate = refundReasonUpdate,
            )
        }
    }
}

@Composable
private fun OrdersDialogs(
    dialogState: WooPosOrderDetailsState.DialogState,
    onIssueRefundDialogDismissed: () -> Unit,
    onRefundDetailsDialogDismissed: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    refundReasonUpdate: String?,
) {
    val retainedDialog = rememberRetained(
        when (dialogState) {
            is WooPosOrderDetailsState.DialogState.IssueRefund -> dialogState
            is WooPosOrderDetailsState.DialogState.RefundDetails -> dialogState
            WooPosOrderDetailsState.DialogState.Hidden -> null
        }
    )

    when (retainedDialog) {
        is WooPosOrderDetailsState.DialogState.IssueRefund -> {
            WooPosIssueRefundDialog(
                orderId = retainedDialog.orderId,
                isVisible = dialogState is WooPosOrderDetailsState.DialogState.IssueRefund,
                onDismissRequest = onIssueRefundDialogDismissed,
                onNavigationEvent = onNavigationEvent,
                refundReasonUpdate = refundReasonUpdate
            )
        }
        is WooPosOrderDetailsState.DialogState.RefundDetails -> {
            WooPosRefundDetailsDialog(
                dialogState = retainedDialog,
                isVisible = dialogState is WooPosOrderDetailsState.DialogState.RefundDetails,
                onDismissRequest = onRefundDetailsDialogDismissed,
            )
        }
        WooPosOrderDetailsState.DialogState.Hidden,
        null -> Unit
    }
}

@Composable
private fun OrderDetailsPane(
    detailState: WooPosOrderDetailsState,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    modifier: Modifier = Modifier,
    showOrderNumber: Boolean = true,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        when (detailState) {
            is WooPosOrderDetailsState.Loaded -> {
                WooPosOrderDetails(
                    modifier = Modifier.fillMaxHeight(),
                    details = detailState.details,
                    showOrderNumber = showOrderNumber,
                    onUIEvent = onUIEvent
                )
            }
            is WooPosOrderDetailsState.Loading -> {
                OrderDetailsLoadingPane(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(
                            start = WooPosSpacing.Medium.value,
                            end = WooPosSpacing.Medium.value,
                            top = WooPosSpacing.XLarge.value,
                            bottom = WooPosSpacing.XLarge.value
                        )
                )
            }
            is WooPosOrderDetailsState.Error -> {
                WooPosErrorScreen(
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(id = R.string.woopos_orders_loading_error_title),
                    reason = detailState.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
                        click = onRetryDetailLoad
                    )
                )
            }
            is WooPosOrderDetailsState.Idle -> {
                WooPosEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    icon = WooPosIcons.OrdersEmpty,
                    title = stringResource(R.string.woopos_orders_no_order_selected),
                    message = "",
                    contentDescription = stringResource(R.string.woopos_orders_empty_list_image_description)
                )
            }
        }
    }
}

@Composable
private fun OrdersListWithDetails(
    listContent: WooPosOrdersListState.Content,
    detailState: WooPosOrderDetailsState,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        OrdersListPane(
            state = listContent,
            scrollToTopEvent = scrollToTopEvent,
            onRefresh = onRefresh,
            isRefreshing = listContent.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            onOrderSelected = onOrderSelected,
            onEndOfOrdersListReached = onEndOfOrdersListReached,
            onPaginationErrorTryAgain = onPaginationErrorTryAgain,
            onSearchEvent = onSearchEvent,
            onSearchErrorRetry = onSearchErrorRetry,
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceBright)
        )
        OrderDetailsPane(
            detailState = detailState,
            onUIEvent = onUIEvent,
            onRetryDetailLoad = onRetryDetailLoad,
            showOrderNumber = true,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun SingleOrderDetails(
    detailState: WooPosOrderDetailsState.Loaded,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit
) {
    OrderDetailsPane(
        detailState = detailState,
        onUIEvent = onUIEvent,
        onRetryDetailLoad = {},
        showOrderNumber = false,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OrdersListPane(
    state: WooPosOrdersListState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = WooPosSpacing.Medium.value)
                .heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
        ) {
            WooPosSearchInput(
                state = state.searchInputState,
                searchIconBackgroundColor = MaterialTheme.colorScheme.surface,
                onEvent = onSearchEvent,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(
                    pullRefreshState,
                    enabled = state.pullToRefreshState != WooPosPullToRefreshState.Disabled
                )
        ) {
            OrdersList(
                modifier = Modifier.fillMaxSize(),
                state = state,
                scrollToTopEvent = scrollToTopEvent,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onSearchErrorRetry = onSearchErrorRetry,
            )

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WooPosSpacing.XSmall.value),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OrdersList(
    modifier: Modifier = Modifier,
    state: WooPosOrdersListState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchErrorRetry: () -> Unit
) {
    when (val items = state.items) {
        is WooPosOrdersListState.Content.Items.Loaded -> {
            LoadedOrdersList(
                modifier = modifier,
                items = items.items,
                paginationState = state.paginationState,
                scrollToTopEvent = scrollToTopEvent,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain
            )
        }

        is WooPosOrdersListState.Content.Items.Searching -> {
            WooPosOrdersListLoadingPane(
                modifier = modifier.imePadding()
            )
        }

        is WooPosOrdersListState.Content.Items.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                WooPosErrorScreen(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = WooPosSpacing.XLarge.value),
                    message = items.title,
                    reason = items.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(id = R.string.retry),
                        click = onSearchErrorRetry
                    )
                )
            }
        }

        is WooPosOrdersListState.Content.Items.NothingFound -> {
            WooPosEmptyScreen(
                modifier = modifier
                    .imePadding()
                    .padding(horizontal = WooPosSpacing.XLarge.value),
                title = items.title,
                message = items.message,
                contentDescription = stringResource(id = R.string.woopos_search_empty_image_content_description)
            )
        }
    }
}

@Composable
private fun LoadedOrdersList(
    modifier: Modifier = Modifier,
    items: List<WooPosOrdersState.OrderItemViewState>,
    paginationState: WooPosPaginationState,
    scrollToTopEvent: SharedFlow<Unit>,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit
) {
    val listState = rememberLazyListState()

    val loadMoreBuffer = 5
    val shouldLoadMore = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
            lastVisible >= total - 1 - loadMoreBuffer
        }
    }
    LaunchedEffect(paginationState) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect { onEndOfOrdersListReached() }
    }

    LaunchedEffect(Unit) {
        scrollToTopEvent.collect {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value),
        state = listState,
    ) {
        items(items, key = { it.id }) { item ->
            WooPosCard(
                modifier = modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                elevation = WooPosElevation.Medium,
                shadowType = ShadowType.Soft,
                isSelected = item.isSelected,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOrderSelected(item.id) }
                        .padding(WooPosSpacing.Medium.value),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        WooPosText(
                            item.title,
                            style = WooPosTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        WooPosText(
                            item.date,
                            style = WooPosTypography.BodySmall,
                            color = WooPosTheme.colors.onSurfaceVariantHighest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        val email = item.customerEmail.orEmpty()
                        if (email.isNotBlank()) {
                            WooPosText(
                                email,
                                style = WooPosTypography.BodySmall,
                                color = WooPosTheme.colors.onSurfaceVariantHighest,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(WooPosSpacing.Small.value))

                        WooPosOrdersStatusBadge(item.status)
                    }

                    WooPosText(
                        text = item.total,
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        if (paginationState == WooPosPaginationState.Loading) {
            item {
                WooPosOrdersOrderLoadingRow()
            }
        }

        if (paginationState == WooPosPaginationState.Error) {
            item {
                OrdersPaginationErrorRow(onPaginationErrorTryAgain)
            }
        }
    }
}

@Composable
private fun OrdersEmpty(
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosEmptyScreen(
        modifier = modifier.fillMaxSize(),
        icon = WooPosIcons.OrdersEmpty,
        title = stringResource(id = R.string.woopos_orders_empty_list_title),
        message = stringResource(id = R.string.woopos_orders_empty_list_message),
        contentDescription = stringResource(id = R.string.woopos_orders_empty_list_image_description),
        actionLabel = stringResource(id = R.string.woopos_orders_empty_action_label),
        onActionClicked = onActionClicked
    )
}

@Composable
private fun OrdersError(
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosErrorScreen(
        modifier = modifier,
        message = stringResource(id = R.string.woopos_orders_loading_error_title),
        reason = stringResource(id = R.string.woopos_orders_loading_error_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
            click = onRetryClicked
        )
    )
}

private fun WooPosOrdersListState.Content.withoutSelection(): WooPosOrdersListState.Content {
    val items = this.items
    if (items !is WooPosOrdersListState.Content.Items.Loaded) return this
    return copy(
        items = WooPosOrdersListState.Content.Items.Loaded(
            items.items.map { it.copy(isSelected = false) }
        )
    )
}

@Composable
private fun OrdersPaginationErrorRow(onPaginationErrorTryAgain: () -> Unit) {
    WooPosPaginationErrorIndicator(
        icon = null,
        message = stringResource(id = R.string.woopos_orders_pagination_error_title),
        description = stringResource(id = R.string.woopos_orders_pagination_error_content_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_pagination_try_again_label),
            click = onPaginationErrorTryAgain
        ),
    )
}

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    val item1 = WooPosOrdersState.OrderItemViewState(
        id = 1,
        title = "#014",
        date = "Aug 28, 2025 at 10:31 AM",
        total = "$17.00",
        customerEmail = "johndoe@mail.com",
        isSelected = true,
        status = PosOrderStatus(
            text = "Completed",
            colorKey = OrderStatusColorKey.COMPLETED
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )
    val item2 = WooPosOrdersState.OrderItemViewState(
        id = 2,
        title = "#013",
        date = "Jul 28, 2025 at 10:31 AM",
        total = "$43.90",
        customerEmail = "johndoe@mail.com",
        isSelected = false,
        status = PosOrderStatus(
            text = "Processing",
            colorKey = OrderStatusColorKey.PROCESSING
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )

    val details1 = sampleOrderDetails(id = 1L, number = "#014")

    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.Loaded(
                    items = listOf(item1, item2)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Loaded(
                details = details1,
                dialogState = WooPosOrderDetailsState.DialogState.Hidden
            ),
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onIssueRefundDialogDismissed = {},
            onRefundDetailsDialogDismissed = {},
            onNavigationEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersSearchErrorStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.Error(
                    title = "Search error",
                    message = "Please try again"
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onIssueRefundDialogDismissed = {},
            onRefundDetailsDialogDismissed = {},
            onNavigationEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersNothingFoundStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.NothingFound(
                    title = "Nothing found",
                    message = "Try a different search"
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onIssueRefundDialogDismissed = {},
            onRefundDetailsDialogDismissed = {},
            onNavigationEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersEmptyStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Empty(
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onIssueRefundDialogDismissed = {},
            onRefundDetailsDialogDismissed = {},
            onNavigationEvent = {},
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun sampleOrderDetails(
    id: Long = 1L,
    number: String = "#014"
) = WooPosOrdersState.OrderDetailsViewState.Computed.Details(
    id = id,
    number = number,
    dateTime = "Aug 28, 2025 at 10:31 AM",
    customerEmail = "johndoe@mail.com",
    status = PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED),
    lineItems = WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState.Loaded(
        listOf(
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 101,
                name = "Cup",
                attributesDescription = null,
                qtyAndUnitPrice = "1 x $8.50",
                lineTotal = "$15.00",
                imageUrl = null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 102,
                name = "Coffee Container",
                attributesDescription = "Blue, Large",
                qtyAndUnitPrice = "1 x $10.00",
                lineTotal = "$8.00",
                imageUrl = null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 103,
                name = "Paper Filter",
                attributesDescription = null,
                qtyAndUnitPrice = "1 x $4.50",
                lineTotal = "$8.00",
                imageUrl = null
            )
        )
    ),
    refundedLineItems = WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState.Loaded(emptyList()),
    breakdown = WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown(
        products = "$23.00",
        discount = "-$5.00",
        discountCode = "8qew4mnq",
        taxes = "$0.00",
        shipping = null,
        refundsState = WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState.Loaded(
            refunds = listOf(
                WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundRow(
                    label = "Refund #1",
                    amount = "-$3.00",
                    date = "Aug 29, 2025 at 12:26 PM",
                    reason = "Customer bought an extra item.",
                ),
                WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundRow(
                    label = "Refund #2",
                    amount = "-$2.00",
                    date = "Aug 30, 2025 at 2:15 PM",
                    reason = null,
                ),
            ),
        ),
        netPayment = "$12.00"
    ),
    total = "$17.00",
    totalPaid = "$17.00",
    paymentMethodTitle = "WooCommerce In-Person Payments",
    actionsState = WooPosOrdersState.OrderActionsState.Loaded(
        listOf(
            WooPosOrdersState.OrderAction.IssueRefund(id),
            WooPosOrdersState.OrderAction.EmailReceipt(id)
        )
    )
)
