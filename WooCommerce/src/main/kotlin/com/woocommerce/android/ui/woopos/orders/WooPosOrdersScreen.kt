package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    WooPosOrdersScreen(
        state = state,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = viewModel::onRefresh,
        onOrderSelected = viewModel::onOrderSelected,
        onEndOfOrdersListReached = viewModel::onEndOfOrdersListReached,
        onPaginationErrorTryAgain = viewModel::onPaginationErrorTryAgain,
        onSearchEvent = viewModel::onSearchEvent,
        onOrdersEmptyActionClicked = viewModel::onOrdersEmptyActionClicked,
        onOrdersLoadingErrorRetryButtonClicked = viewModel::onOrdersLoadingErrorRetryButtonClicked
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
    onOrdersEmptyActionClicked: () -> Unit,
    onOrdersLoadingErrorRetryButtonClicked: () -> Unit
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
                onSearchEvent = onSearchEvent
            )
            is WooPosOrdersState.Empty -> OrdersEmpty(
                onActionClicked = onOrdersEmptyActionClicked
            )

            is WooPosOrdersState.Error -> OrdersError(
                onRetryClicked = onOrdersLoadingErrorRetryButtonClicked
            )

            is WooPosOrdersState.Loading -> {
            }
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
    onSearchEvent: (WooPosSearchUIEvent) -> Unit
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
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        )

        OrderDetails(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            order = state.items.find { it.isSelected },
        )
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
    LaunchedEffect(state.paginationState) {
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
        items(state.items, key = { it.id }) { item ->
            WooPosCard(
                modifier = modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                elevation = WooPosElevation.Medium,
                shadowType = ShadowType.Soft,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .then(
                            if (item.isSelected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = Color.Black,
                                    shape = MaterialTheme.shapes.medium
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onOrderSelected(item.id) }
                        .semantics { selected = item.isSelected }
                        .padding(WooPosSpacing.Medium.value),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
                    ) {
                        WooPosText(
                            item.title,
                            style = WooPosTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        WooPosText(
                            item.date,
                            style = WooPosTypography.BodySmall,
                            color = WooPosTheme.colors.onSurfaceVariantHighest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

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

                        OrderStatusBadge(item.status)
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

        if (state.paginationState == WooPosPaginationState.Loading) {
            item {
                OrdersPaginationLoadingRow()
            }
        }

        if (state.paginationState == WooPosPaginationState.Error) {
            item {
                OrdersPaginationErrorRow(onPaginationErrorTryAgain)
            }
        }
    }
}

@Composable
private fun OrderDetails(
    modifier: Modifier = Modifier,
    order: OrderItemViewState?,
) {
    Column(modifier = modifier.fillMaxSize()) {
        WooPosToolbar(
            modifier = Modifier.fillMaxWidth(),
            titleText = order?.title ?: "--",
            titleFontWeight = FontWeight.Bold
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = WooPosSpacing.Large.value, end = WooPosSpacing.Large.value)
        ) {
            WooPosText(
                text = "Order details goes here",
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrdersPaginationLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Medium.value
            )
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)) {
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(18.dp)
            )
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        WooPosShimmerBox(
            modifier = Modifier
                .width(72.dp)
                .height(18.dp)
                .alignByBaseline()
        )
    }
}

@Composable
fun OrdersEmpty(
    onActionClicked: () -> Unit
) {
    WooPosEmptyScreen(
        modifier = Modifier.fillMaxSize(),
        icon = painterResource(id = R.drawable.ic_woo_pos_orders_empty),
        title = stringResource(id = R.string.woopos_orders_empty_list_title),
        message = stringResource(id = R.string.woopos_orders_empty_list_message),
        contentDescription = stringResource(id = R.string.woopos_coupons_empty_list_image_description),
        actionLabel = stringResource(id = R.string.woopos_orders_empty_action_label),
        onActionClicked = onActionClicked
    )
}

@Composable
fun OrdersError(
    onRetryClicked: () -> Unit
) {
    WooPosErrorScreen(
        message = stringResource(id = R.string.woopos_orders_loading_error_title),
        reason = stringResource(id = R.string.woopos_orders_loading_error_message),
        primaryButton = Button(
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
        primaryButton = Button(
            text = stringResource(id = R.string.woopos_coupons_pagination_try_again_label),
            click = onPaginationErrorTryAgain
        ),
    )
}

@Composable
fun OrderStatusBadge(status: PosOrderStatus) {
    val bgColor = when (status.colorKey) {
        OrderStatusColorKey.COMPLETED -> WooPosTheme.colors.infoLowest
        OrderStatusColorKey.FAILED -> WooPosTheme.colors.errorLowest
        else -> WooPosTheme.colors.default
    }

    val textColor = when (status.colorKey) {
        OrderStatusColorKey.COMPLETED -> WooPosTheme.colors.onInfoLowest
        OrderStatusColorKey.FAILED -> WooPosTheme.colors.onErrorLowest
        else -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.text,
        style = WooPosTypography.BodySmall,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .background(bgColor)
            .padding(horizontal = WooPosSpacing.Small.value, vertical = WooPosSpacing.XSmall.value)
    )
}

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            state = WooPosOrdersState.Content(
                items = listOf(
                    OrderItemViewState(
                        id = 1,
                        title = "#1234",
                        date = "Jan 15, 2025 at 10:30 AM",
                        total = "$125.00",
                        customerEmail = "customer@example.com",
                        isSelected = true,
                        status = PosOrderStatus(
                            text = "Completed",
                            colorKey = OrderStatusColorKey.COMPLETED
                        )
                    ),
                    OrderItemViewState(
                        id = 2,
                        title = "#1235",
                        date = "Jan 16, 2025 at 2:45 PM",
                        total = "$89.50",
                        customerEmail = "another@example.com",
                        isSelected = false,
                        status = PosOrderStatus(
                            text = "Processing",
                            colorKey = OrderStatusColorKey.PROCESSING
                        )
                    )
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
                paginationState = WooPosPaginationState.None,
                selectedOrderId = 1
            ),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {}
        )
    }
}
