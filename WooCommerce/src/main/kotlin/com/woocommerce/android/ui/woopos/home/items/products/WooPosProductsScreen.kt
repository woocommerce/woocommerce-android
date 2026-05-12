package com.woocommerce.android.ui.woopos.home.items.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosCustomAmountEntryRow
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsUIEvent.EndOfItemsListReached
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsUIEvent.PullToRefreshTriggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosProductsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    val productsViewModel: WooPosProductsViewModel = hiltViewModel()
    WooPosProductsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        listState = listState,
        onUIEvent = { productsViewModel.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosProductsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosProductsViewState>,
    listState: LazyListState,
    onUIEvent: (WooPosProductsUIEvent) -> Unit,
) {
    val state = itemsStateFlow.collectAsState()

    ProductsList(
        modifier = modifier,
        state = state,
        listState = listState,
        onItemClicked = { item ->
            onUIEvent(WooPosProductsUIEvent.ItemClicked(item))
        },
        onEndOfItemListReached = { onUIEvent(EndOfItemsListReached) },
        onRetryClicked = { onUIEvent(ProductsLoadingErrorRetryButtonClicked) },
        onPullToRefreshTriggered = { onUIEvent(PullToRefreshTriggered) },
        onCustomAmountEntryRowClicked = {
            onUIEvent(WooPosProductsUIEvent.CustomAmountEntryRowClicked)
        },
    )
}

@ExperimentalMaterialApi
@Composable
private fun ProductsList(
    modifier: Modifier,
    state: State<WooPosProductsViewState>,
    listState: LazyListState,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onRetryClicked: () -> Unit,
    onPullToRefreshTriggered: () -> Unit,
    onCustomAmountEntryRowClicked: () -> Unit = {},
) {
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = state.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
        onRefresh = onPullToRefreshTriggered,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(
                state = pullToRefreshState,
                enabled = state.value.pullToRefreshState == WooPosPullToRefreshState.Enabled,
            )
    ) {
        Column(
            Modifier.fillMaxHeight()
        ) {
            when (val itemsState = state.value) {
                is WooPosProductsViewState.Content -> {
                    Content(
                        itemsState = itemsState,
                        listState = listState,
                        onItemClicked = onItemClicked,
                        onEndOfItemListReached = onEndOfItemListReached,
                        onCustomAmountEntryRowClicked = onCustomAmountEntryRowClicked,
                    )
                }

                is WooPosProductsViewState.Loading -> WooPosItemsLoadingIndicator(
                    modifier = Modifier.padding(top = WooPosSpacing.Large.value)
                )

                is WooPosProductsViewState.Empty -> WooPosEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(id = R.string.woopos_products_empty_list_title),
                    message = stringResource(id = R.string.woopos_products_empty_list_message),
                    contentDescription = stringResource(id = R.string.woopos_products_empty_list_image_description),
                )

                is WooPosProductsViewState.Error -> ProductsError { onRetryClicked() }
            }
        }
        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            state = pullToRefreshState
        )
    }
}

@Composable
private fun Content(
    itemsState: WooPosProductsViewState.Content,
    listState: LazyListState,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onCustomAmountEntryRowClicked: () -> Unit,
) {
    WooPosItemList(
        modifier = Modifier.padding(top = WooPosSpacing.XSmall.value),
        state = itemsState,
        listState = listState,
        headerContent = {
            WooPosCustomAmountEntryRow(
                onClick = onCustomAmountEntryRowClicked,
            )
        },
        onItemClicked = onItemClicked,
        onEndOfProductsListReached = onEndOfItemListReached,
    ) {
        ProductsPaginationError(
            onRetryClicked = {
                onEndOfItemListReached()
            }
        )
    }
}

@Composable
fun ProductsError(onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorScreen(
            message = stringResource(id = R.string.woopos_products_loading_error_title),
            reason = stringResource(id = R.string.woopos_products_loading_error_message),
            primaryButton = WooPosErrorScreenButtonState(
                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                click = onRetryClicked
            )
        )
    }
}

@Composable
private fun ProductsPaginationError(onRetryClicked: () -> Unit) {
    WooPosPaginationErrorIndicator(
        message = stringResource(id = R.string.woopos_items_pagination_error_title),
        description = stringResource(id = R.string.woopos_items_pagination_error_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
            click = onRetryClicked
        ),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            items = listOf(
                Product.Simple(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                Product.Simple(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                Product.Variable(
                    3,
                    name = "Product 3",
                    price = "2000.00$",
                    imageUrl = null,
                    numOfVariations = 20,
                    variationIds = listOf()
                ),
                Product.Simple(
                    4,
                    name = "Product 4",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            paginationState = WooPosPaginationState.Loading,
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenPaginationErrorPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            items = listOf(
                Product.Simple(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                Product.Simple(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                Product.Variable(
                    3,
                    name = "Product 3",
                    price = "2000.00$",
                    imageUrl = null,
                    numOfVariations = 20,
                    variationIds = listOf()
                ),
            ),
            paginationState = WooPosPaginationState.Error,
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenLoadingPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Loading(
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosProductsScreenEmptyListPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Empty(
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosProductsScreenErrorPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Error()
    )
    WooPosTheme {
        WooPosProductsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosHomeScreenItemsWithInfoIconInToolbarPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            items = listOf(
                Product.Simple(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                Product.Simple(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                Product.Simple(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            pullToRefreshState = WooPosPullToRefreshState.Disabled,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}
