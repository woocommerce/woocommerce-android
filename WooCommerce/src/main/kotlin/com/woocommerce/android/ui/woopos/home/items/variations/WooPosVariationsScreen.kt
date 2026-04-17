package com.woocommerce.android.ui.woopos.home.items.variations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.adaptiveContentWidth
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WooPosVariationsScreen(
    modifier: Modifier,
    variableProductData: WooPosVariationsNavigationData,
    onBackClicked: () -> Unit
) {
    val viewModel: WooPosVariationsViewModel = hiltViewModel(
        key = variableProductData.id.toString()
    )
    LaunchedEffect(variableProductData.id) {
        viewModel.init(variableProductData.id, variableProductData.sourceType)
    }
    val state = viewModel.viewState
    WooPosVariationsScreens(
        modifier,
        onBackClicked,
        onItemClicked = { productId, variationId ->
            viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(productId, variationId))
        },
        onEndOfItemListReached = {
            viewModel.onUIEvent(
                WooPosVariationsUIEvents.EndOfItemsListReached(
                    variableProductData.id,
                    variableProductData.numOfVariations
                )
            )
        },
        onPullToRefresh = {
            viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(variableProductData.id))
        },
        onRetryClicked = {
            viewModel.onUIEvent(
                WooPosVariationsUIEvents.VariationsLoadingErrorRetryButtonClicked(variableProductData.id)
            )
        },
        state,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosVariationsScreens(
    modifier: Modifier,
    onBackClicked: () -> Unit,
    onItemClicked: (Long, Long) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRetryClicked: () -> Unit,
    state: StateFlow<WooPosVariationsViewState>,
) {
    val itemState = state.collectAsState()
    val pullToRefreshState = rememberPullRefreshState(
        refreshing = itemState.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
        onRefresh = onPullToRefresh
    )
    val listState = rememberLazyListState()
    BackHandler(onBack = onBackClicked)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(
                state = pullToRefreshState,
                enabled = itemState.value.pullToRefreshState == WooPosPullToRefreshState.Enabled
            )
    ) {
        when (val itemsState = itemState.value) {
            is WooPosVariationsViewState.Content -> {
                WooPosItemList(
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value),
                    state = itemsState,
                    listState = listState,
                    onItemClicked = {
                        onItemClicked(
                            (it as WooPosItemSelectionViewState.Product.Variation).productId,
                            it.id
                        )
                    },
                    onEndOfProductsListReached = onEndOfItemListReached,
                    onErrorWhilePaginating = {
                        VariationsPaginationError {
                            onEndOfItemListReached()
                        }
                    }
                )
            }

            is WooPosVariationsViewState.Loading -> {
                WooPosItemsLoadingIndicator()
            }

            is WooPosVariationsViewState.Error -> {
                VariationsError(
                    modifier = Modifier.adaptiveContentWidth()
                ) {
                    onRetryClicked()
                }
            }

            is WooPosVariationsViewState.Empty -> {
                WooPosEmptyScreen(
                    modifier = Modifier.fillMaxSize()
                        .padding(top = WooPosSpacing.Large.value),
                    title = stringResource(id = R.string.woopos_variations_empty_list_title),
                    message = stringResource(id = R.string.woopos_variations_empty_list_message),
                    contentDescription = stringResource(
                        id = R.string.woopos_variations_empty_list_image_description
                    )
                )
            }
        }

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = itemState.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            state = pullToRefreshState
        )
    }
}

@Composable
private fun VariationsError(modifier: Modifier, onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorScreen(
            modifier = modifier,
            message = stringResource(id = R.string.woopos_variations_loading_error_title),
            reason = stringResource(id = R.string.woopos_variations_loading_error_message),
            primaryButton = WooPosErrorScreenButtonState(
                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                click = onRetryClicked
            )
        )
    }
}

@Composable
private fun VariationsPaginationError(onRetryClicked: () -> Unit) {
    WooPosPaginationErrorIndicator(
        message = stringResource(id = R.string.woopos_items_pagination_error_title),
        description = stringResource(id = R.string.woopos_items_pagination_error_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
            click = onRetryClicked
        ),
    )
}

@Composable
@WooPosPreview
fun WooPosVariationsScreenPreview() {
    val productState = MutableStateFlow(
        WooPosVariationsViewState.Content(
            items = listOf(
                WooPosItemSelectionViewState.Product.Variation(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    productId = 1,
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosItemSelectionViewState.Product.Variation(
                    2,
                    name = "Product 2",
                    productId = 1,
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosItemSelectionViewState.Product.Variation(
                    3,
                    name = "Product 3",
                    productId = 1,
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
        )
    )
    WooPosTheme {
        WooPosVariationsScreens(
            modifier = Modifier,
            onBackClicked = {},
            onItemClicked = { _, _ -> },
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            state = productState,
        )
    }
}
