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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

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
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        )

        WooPosOrderDetails(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            details = state.selectedDetails,
            onEmailReceiptButtonClicked = onEmailReceiptButtonClicked
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
        items(state.items.keys.toList(), key = { it.id }) { item ->
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
                WooPosOrdersOrderLoadingRow()
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
fun OrdersEmpty(
    onActionClicked: () -> Unit
) {
    WooPosEmptyScreen(
        modifier = Modifier.fillMaxSize(),
        icon = WooPosIcons.OrdersEmpty,
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
        OrderStatusColorKey.PROCESSING,
        OrderStatusColorKey.ON_HOLD,
        OrderStatusColorKey.OTHER -> WooPosTheme.colors.default
    }

    val textColor = when (status.colorKey) {
        OrderStatusColorKey.COMPLETED -> WooPosTheme.colors.onInfoLowest
        OrderStatusColorKey.FAILED -> WooPosTheme.colors.onErrorLowest
        OrderStatusColorKey.PROCESSING,
        OrderStatusColorKey.ON_HOLD,
        OrderStatusColorKey.OTHER -> WooPosTheme.colors.onDefault
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
        )
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
        )
    )

    val details1 = sampleOrderDetails(id = 1L, number = "#014")
    val details2 = sampleOrderDetails(id = 2L, number = "#013")

    WooPosTheme {
        WooPosOrdersScreen(
            state = WooPosOrdersState.Content(
                items = mapOf(
                    item1 to details1,
                    item2 to details2
                ),
                selectedDetails = details1,
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
                paginationState = WooPosPaginationState.None
            ),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
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
