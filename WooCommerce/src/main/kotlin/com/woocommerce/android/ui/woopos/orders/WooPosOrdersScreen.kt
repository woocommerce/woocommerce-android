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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsEmptyList
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    if (state is WooPosOrdersState.Error) {
        OrdersError(
            onRetryClicked = viewModel::onOrdersLoadingErrorRetryButtonClicked
        )
        return
    }

    if (state is WooPosOrdersState.Empty) {
        OrdersEmpty(
            onActionClicked = { viewModel::onOrdersEmptyActionClicked }
        )
        return
    }

    Row(modifier = Modifier.fillMaxSize()) {
        OrdersList(
            state = state,
            onBackClicked = onBackClicked,
            onRefresh = viewModel::onRefresh,
            isRefreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            onOrderSelected = viewModel::onOrderSelected,
            onEndOfOrdersListReached = viewModel::onEndOfOrdersListReached,
            viewModel::onPaginationErrorTryAgain,
            onSearchEvent = viewModel::onSearchEvent,
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        )

        OrderDetails(
            state = state,
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OrdersList(
    state: WooPosOrdersState,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
        ) {
            val (toolbar, searchInput) = createRefs()

            if (state.searchInputState is WooPosSearchInputState.Closed) {
                WooPosToolbar(
                    titleText = stringResource(R.string.woopos_orders_title),
                    onBackClicked = onBackClicked,
                    modifier = Modifier.constrainAs(toolbar) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                )
            }
            WooPosSearchInput(
                state = state.searchInputState,
                onEvent = onSearchEvent,
                modifier = Modifier
                    .statusBarsPadding()
                    .constrainAs(searchInput) {
                        if (state.searchInputState is WooPosSearchInputState.Open) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = androidx.constraintlayout.compose.Dimension.fillToConstraints
                        } else {
                            end.linkTo(parent.end)
                        }
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
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
            when (state) {
                is WooPosOrdersState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = stringResource(R.string.loading),
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Content -> {
                    WooPosOrdersListPaneScreen(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onOrderSelected = onOrderSelected,
                        onEndOfOrdersListReached = onEndOfOrdersListReached,
                        onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                    )
                }

                is WooPosOrdersState.Empty -> { /* handled full-screen in parent */ }
                is WooPosOrdersState.Error -> { /* handled full-screen in parent */ }
            }

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
private fun OrderDetails(
    state: WooPosOrdersState,
    modifier: Modifier = Modifier
) {
    val selectedItem: OrderItemViewState? = when (state) {
        is WooPosOrdersState.Content -> state.items.firstOrNull { it.id == state.selectedOrderId }
        else -> null
    }

    WooPosOrdersDetailPaneScreen(
        selected = selectedItem,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun WooPosOrdersListPaneScreen(
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

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = WooPosSpacing.XSmall.value)
    ) {
        items(state.items, key = { it.id }) { item ->
            val isSelected = item.id == state.selectedOrderId
            val background = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
            val foreground = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(background)
                    .clickable { onOrderSelected(item.id) }
                    .semantics { selected = isSelected }
                    .padding(
                        horizontal = WooPosSpacing.Medium.value,
                        vertical = WooPosSpacing.Medium.value
                    ),
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)) {
                    WooPosText(item.title, style = WooPosTypography.BodyMedium, color = foreground)
                    WooPosText(item.date, style = WooPosTypography.BodySmall, color = foreground)
                }
                Spacer(Modifier.weight(1f))
                WooPosText(
                    text = item.total,
                    style = WooPosTypography.BodyMedium,
                    modifier = Modifier.alignByBaseline()
                )
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
fun WooPosOrdersDetailPaneScreen(
    selected: OrderItemViewState?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        WooPosToolbar(
            modifier = Modifier.fillMaxWidth(),
            titleText = selected?.title ?: "--",
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
fun OrdersEmpty(onActionClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosItemsEmptyList(
            modifier = Modifier.fillMaxSize(),
            icon = painterResource(id = R.drawable.ic_woo_pos_orders_empty),
            title = stringResource(id = R.string.woopos_orders_empty_list_title),
            message = stringResource(id = R.string.woopos_orders_empty_list_message),
            contentDescription = stringResource(id = R.string.woopos_coupons_empty_list_image_description),
            actionLabel = stringResource(id = R.string.woopos_orders_empty_action_label),
            onActionClicked = { onActionClicked() }
        )
    }
}

@Composable
fun OrdersError(onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
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

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    WooPosTheme { WooPosOrdersScreen(onNavigationEvent = {}) }
}
