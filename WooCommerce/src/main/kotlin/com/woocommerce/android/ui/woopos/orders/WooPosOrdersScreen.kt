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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    val onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Row(modifier = Modifier.fillMaxSize()) {
        OrdersList(
            state = state,
            onBackClicked = onBackClicked,
            onRefresh = viewModel::onRefresh,
            isRefreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            onOrderSelected = viewModel::onOrderSelected,
            onEndOfOrdersListReached = viewModel::onEndOfOrdersListReached,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_orders_title),
            onBackClicked = onBackClicked,
        )

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

                is WooPosOrdersState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = state.message,
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WooPosText(
                            text = "No orders found",
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(WooPosSpacing.Large.value)
                        )
                    }
                }

                is WooPosOrdersState.Content -> {
                    val isLoadingMore = state.paginationState == WooPosPaginationState.Loading
                    val canLoadMore = state.paginationState == WooPosPaginationState.None &&
                        state.pullToRefreshState != WooPosPullToRefreshState.Refreshing

                    WooPosOrdersListPaneScreen(
                        items = state.items,
                        selectedOrderId = state.selectedOrderId,
                        onOrderSelected = onOrderSelected,
                        onEndOfOrdersListReached = onEndOfOrdersListReached,
                        canLoadMore = canLoadMore && state.pullToRefreshState != WooPosPullToRefreshState.Refreshing,
                        isLoadingMore = isLoadingMore,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
    items: List<OrderItemViewState>,
    selectedOrderId: Long?,
    onOrderSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onEndOfOrdersListReached: () -> Unit,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreBuffer: Int = 5,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
            lastVisible >= total - 1 - loadMoreBuffer
        }
    }
    LaunchedEffect(canLoadMore, isLoadingMore, listState) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it && canLoadMore && !isLoadingMore }
            .collect { onEndOfOrdersListReached() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = WooPosSpacing.XSmall.value)
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id == selectedOrderId
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

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    WooPosTheme { WooPosOrdersScreen(onNavigationEvent = {}) }
}
