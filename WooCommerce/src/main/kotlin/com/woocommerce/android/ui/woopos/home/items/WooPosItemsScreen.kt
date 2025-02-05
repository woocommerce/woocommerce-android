package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.SimpleProduct
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.VariableProduct
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.EndOfItemsListReached
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.ProductsLoadingErrorRetryButtonClicked
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.PullToRefreshTriggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    val productsViewModel: WooPosItemsViewModel = hiltViewModel()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        listState,
        onItemClicked = { item ->
            productsViewModel.onUIEvent(WooPosItemsUIEvent.ItemClicked(item))
        },
        onEndOfItemListReached = { productsViewModel.onUIEvent(EndOfItemsListReached) },
        onPullToRefresh = { productsViewModel.onUIEvent(PullToRefreshTriggered) },
        onRetryClicked = { productsViewModel.onUIEvent(ProductsLoadingErrorRetryButtonClicked) },
        onSimpleProductsBannerClosed = {
            productsViewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerClosed)
        },
        onSimpleProductsBannerLearnMoreClicked = {
            productsViewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerLearnMoreClicked)
        },
        onToolbarInfoIconClicked = {
            productsViewModel.onUIEvent(WooPosItemsUIEvent.SimpleProductsDialogInfoIconClicked)
        },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosItemsViewState>,
    listState: LazyListState,
    onItemClicked: (item: WooPosItem) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRetryClicked: () -> Unit,
    onSimpleProductsBannerClosed: () -> Unit,
    onSimpleProductsBannerLearnMoreClicked: () -> Unit,
    onToolbarInfoIconClicked: () -> Unit,
) {
    val state = itemsStateFlow.collectAsState()
    val pullToRefreshState = rememberPullRefreshState(state.value.reloadingProductsWithPullToRefresh, onPullToRefresh)

    MainItemsList(
        modifier = modifier,
        pullToRefreshState = pullToRefreshState,
        state = state,
        listState = listState,
        onToolbarInfoIconClicked = onToolbarInfoIconClicked,
        onSimpleProductsBannerLearnMoreClicked = onSimpleProductsBannerLearnMoreClicked,
        onSimpleProductsBannerClosed = onSimpleProductsBannerClosed,
        onItemClicked = onItemClicked,
        onEndOfItemListReached = onEndOfItemListReached,
        onRetryClicked = onRetryClicked
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    pullToRefreshState: PullRefreshState,
    state: State<WooPosItemsViewState>,
    listState: LazyListState,
    onToolbarInfoIconClicked: () -> Unit,
    onSimpleProductsBannerLearnMoreClicked: () -> Unit,
    onSimpleProductsBannerClosed: () -> Unit,
    onItemClicked: (item: WooPosItem) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onRetryClicked: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullToRefreshState)
            .padding(
                start = 16.dp.toAdaptivePadding(),
                end = 16.dp.toAdaptivePadding(),
                top = 40.dp.toAdaptivePadding(),
                bottom = 0.dp.toAdaptivePadding(),
            )
    ) {
        Column(
            modifier.fillMaxHeight()
        ) {
            val titleColor = when (state.value) {
                is WooPosItemsViewState.Loading,
                is WooPosItemsViewState.Empty,
                is WooPosItemsViewState.Error -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

                is WooPosItemsViewState.Content -> MaterialTheme.colors.onSurface
            }
            ItemsToolbar(state.value, titleColor, onToolbarInfoIconClicked)

            Spacer(modifier = Modifier.height(24.dp))

            when (val itemsState = state.value) {
                is WooPosItemsViewState.Content -> {
                    Column {
                        SimpleProductsBanner(
                            itemsState.bannerState,
                            onSimpleProductsBannerLearnMoreClicked,
                            onSimpleProductsBannerClosed
                        )
                        WooPosItemList(
                            itemsState,
                            listState,
                            onItemClicked,
                            onEndOfItemListReached,
                        ) {
                            ProductsPaginationError(
                                onRetryClicked = {
                                    onEndOfItemListReached()
                                }
                            )
                        }
                    }
                }

                is WooPosItemsViewState.Loading -> ItemsLoadingIndicator()

                is WooPosItemsViewState.Empty -> ItemsEmptyList(
                    title = stringResource(id = R.string.woopos_products_empty_list_title),
                    message = stringResource(id = R.string.woopos_products_empty_list_message),
                    contentDescription = stringResource(id = R.string.woopos_products_empty_list_image_description),
                )

                is WooPosItemsViewState.Error -> ProductsError { onRetryClicked() }
            }
        }
        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.value.reloadingProductsWithPullToRefresh,
            state = pullToRefreshState
        )
    }
}

@Composable
private fun ItemsToolbar(
    productViewState: WooPosItemsViewState,
    titleColor: Color,
    onToolbarInfoIconClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(id = R.string.woopos_products_screen_title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        when (productViewState) {
            is WooPosItemsViewState.Content -> {
                if (productViewState.bannerState.isBannerHiddenByUser) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            onToolbarInfoIconClicked()
                        }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.info),
                            contentDescription = stringResource(
                                id = R.string.woopos_banner_simple_products_info_content_description
                            ),
                            tint = MaterialTheme.colors.onSurface.copy(ContentAlpha.high),
                        )
                    }
                }
            }

            else -> {
                // no op
            }
        }
    }
}

@Composable
private fun SimpleProductsBanner(
    bannerState: WooPosItemsViewState.Content.BannerState,
    onSimpleProductsBannerLearnMoreClicked: () -> Unit,
    onSimpleProductsBannerClosed: () -> Unit
) {
    AnimatedVisibility(
        visible = !bannerState.isBannerHiddenByUser,
        exit = shrinkVertically(),
    ) {
        WooPosBanner(
            title = stringResource(id = bannerState.title),
            message = stringResource(id = bannerState.message),
            bannerIcon = R.drawable.info,
            onClose = {
                onSimpleProductsBannerClosed()
            },
            onLearnMore = {
                onSimpleProductsBannerLearnMoreClicked()
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
            modifier = Modifier.width(640.dp),
            message = stringResource(id = R.string.woopos_products_loading_error_title),
            reason = stringResource(id = R.string.woopos_products_loading_error_message),
            primaryButton = Button(
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
        primaryButton = Button(
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
        WooPosItemsViewState.Content(
            items = listOf(
                SimpleProduct(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                VariableProduct(
                    3,
                    name = "Product 3",
                    price = "2000.00$",
                    imageUrl = null,
                    numOfVariations = 20,
                    variationIds = listOf()
                ),
                SimpleProduct(
                    4,
                    name = "Product 4",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            paginationState = PaginationState.Loading,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenPaginationErrorPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsViewState.Content(
            items = listOf(
                SimpleProduct(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                VariableProduct(
                    3,
                    name = "Product 3",
                    price = "2000.00$",
                    imageUrl = null,
                    numOfVariations = 20,
                    variationIds = listOf()
                ),
            ),
            paginationState = PaginationState.Error,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenLoadingPreview() {
    val productState = MutableStateFlow(
        WooPosItemsViewState.Loading(
            reloadingProductsWithPullToRefresh = true,
            withCart = false
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosProductsScreenEmptyListPreview() {
    val productState = MutableStateFlow(WooPosItemsViewState.Empty(true))
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosProductsScreenErrorPreview() {
    val productState = MutableStateFlow(WooPosItemsViewState.Error())
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosHomeScreenItemsWithSimpleProductsOnlyBannerPreview() {
    val productState = MutableStateFlow(
        WooPosItemsViewState.Content(
            items = listOf(
                SimpleProduct(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = false,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            )
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosHomeScreenItemsWithInfoIconInToolbarPreview() {
    val productState = MutableStateFlow(
        WooPosItemsViewState.Content(
            items = listOf(
                SimpleProduct(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                SimpleProduct(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            reloadingProductsWithPullToRefresh = false,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            )
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}
