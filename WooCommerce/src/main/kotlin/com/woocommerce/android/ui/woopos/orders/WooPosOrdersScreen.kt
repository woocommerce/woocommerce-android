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
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    navigatedFromEmailReceiptSent: Boolean,
) {
    val viewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    if (navigatedFromEmailReceiptSent) {
        viewModel.onBackFromSuccessfullySendingEmailReceipt()
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    WooPosOrdersScreen(
        state = state,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = viewModel::onRefresh,
        onOrderSelected = viewModel::onOrderSelected,
        onEndOfOrdersListReached = viewModel::onEndOfOrdersListReached,
        onPaginationErrorTryAgain = viewModel::onPaginationErrorTryAgain,
        onSearchEvent = viewModel::onSearchEvent,
        onSearchErrorRetry = viewModel::onSearchErrorRetry,
        onOrdersEmptyActionClicked = viewModel::onOrdersEmptyActionClicked,
        onOrdersLoadingErrorRetryButtonClicked = viewModel::onOrdersLoadingErrorRetryButtonClicked,
        onEmailReceiptButtonClicked = viewModel::onEmailReceiptButtonClicked
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosOrdersScreen(
    state: WooPosOrdersState,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onOrdersEmptyActionClicked: () -> Unit,
    onOrdersLoadingErrorRetryButtonClicked: () -> Unit,
    onEmailReceiptButtonClicked: (Long) -> Unit,
) {
    BackHandler { onBackClicked() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is WooPosOrdersState.Content -> OrdersContent(
                state = currentState,
                onRefresh = onRefresh,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onSearchEvent = onSearchEvent,
                onSearchErrorRetry = onSearchErrorRetry,
                onEmailReceiptButtonClicked = onEmailReceiptButtonClicked
            )

            is WooPosOrdersState.Empty -> OrdersEmpty(
                onActionClicked = onOrdersEmptyActionClicked
            )

            is WooPosOrdersState.Error -> OrdersError(
                onRetryClicked = onOrdersLoadingErrorRetryButtonClicked
            )

            is WooPosOrdersState.Loading -> WooPosOrdersLoadingState()
        }

        if (state.searchInputState is WooPosSearchInputState.Closed) {
            WooPosToolbar(
                titleText = stringResource(R.string.woopos_orders_title),
                onBackClicked = onBackClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OrdersContent(
    state: WooPosOrdersState.Content,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onEmailReceiptButtonClicked: (Long) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        OrdersListPane(
            state = state,
            onRefresh = onRefresh,
            isRefreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
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

        Box(
            modifier = Modifier
                .weight(0.7f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WooPosOrderDetails(
                modifier = Modifier
                    .fillMaxHeight(),
                details = state.selectedDetails,
                onEmailReceiptButtonClicked = onEmailReceiptButtonClicked
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OrdersListPane(
    state: WooPosOrdersState.Content,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = WooPosSpacing.Medium.value)
                .heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
        ) {
            WooPosSearchInput(
                state = state.searchInputState,
                onEvent = onSearchEvent,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.CenterEnd)
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
    state: WooPosOrdersState.Content,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchErrorRetry: () -> Unit
) {
    when (val items = state.items) {
        is WooPosOrdersState.Content.Items.Loaded -> {
            LoadedOrdersList(
                modifier = modifier,
                items = items.items,
                paginationState = state.paginationState,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain
            )
        }

        is WooPosOrdersState.Content.Items.Searching -> {
            WooPosOrdersListLoadingPane(
                modifier = modifier.imePadding()
            )
        }

        is WooPosOrdersState.Content.Items.Error -> {
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

        is WooPosOrdersState.Content.Items.NothingFound -> {
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
    items: Map<OrderItemViewState, OrderDetailsViewState>,
    paginationState: WooPosPaginationState,
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

    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value),
        state = listState,
    ) {
        items(items.keys.toList(), key = { it.id }) { item ->
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
    onActionClicked: () -> Unit
) {
    WooPosEmptyScreen(
        modifier = Modifier.fillMaxSize(),
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
    onRetryClicked: () -> Unit
) {
    WooPosErrorScreen(
        message = stringResource(id = R.string.woopos_orders_loading_error_title),
        reason = stringResource(id = R.string.woopos_orders_loading_error_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
            click = onRetryClicked
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
    val item1 = OrderItemViewState(
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
    val item2 = OrderItemViewState(
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
    val details2 = sampleOrderDetails(id = 2L, number = "#013")

    WooPosTheme {
        WooPosOrdersScreen(
            state = WooPosOrdersState.Content(
                items = WooPosOrdersState.Content.Items.Loaded(
                    items = mapOf(
                        item1 to details1,
                        item2 to details2
                    )
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
                selectedDetails = details1,
                paginationState = WooPosPaginationState.None
            ),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onEmailReceiptButtonClicked = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersSearchErrorStatePreview() {
    val details = sampleOrderDetails()
    WooPosTheme {
        WooPosOrdersScreen(
            state = WooPosOrdersState.Content(
                items = WooPosOrdersState.Content.Items.Error(
                    title = stringResource(R.string.woopos_search_orders_error_title),
                    message = stringResource(R.string.woopos_search_orders_error_description)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                selectedDetails = details,
                paginationState = WooPosPaginationState.None
            ),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onEmailReceiptButtonClicked = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersNothingFoundStatePreview() {
    val details = sampleOrderDetails()
    WooPosTheme {
        WooPosOrdersScreen(
            state = WooPosOrdersState.Content(
                items = WooPosOrdersState.Content.Items.NothingFound(
                    title = stringResource(R.string.woopos_search_orders_empty_title),
                    message = stringResource(R.string.woopos_search_orders_empty_description)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                selectedDetails = details,
                paginationState = WooPosPaginationState.None
            ),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onEmailReceiptButtonClicked = {}
        )
    }
}

@Suppress("MagicNumber")
private fun sampleOrderDetails(
    id: Long = 1L,
    number: String = "#014"
) = OrderDetailsViewState(
    id = id,
    number = number,
    dateTime = "Aug 28, 2025 at 10:31 AM",
    customerEmail = "johndoe@mail.com",
    status = PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED),
    lineItems = listOf(
        OrderDetailsViewState.LineItemRow(101, "Cup", "1 x $8.50", "$15.00", null),
        OrderDetailsViewState.LineItemRow(102, "Coffee Container", "1 x $10.00", "$8.00", null),
        OrderDetailsViewState.LineItemRow(103, "Paper Filter", "1 x $4.50", "$8.00", null)
    ),
    breakdown = OrderDetailsViewState.TotalsBreakdown(
        products = "$23.00",
        discount = "-$5.00",
        discountCode = "8qew4mnq",
        taxes = "$0.00",
        shipping = null,
        refunds = listOf("-$3.00", "-$2.00"),
        netPayment = "$12.00"
    ),
    total = "$17.00",
    totalPaid = "$17.00",
    paymentMethodTitle = "WooCommerce In-Person Payments"
)
