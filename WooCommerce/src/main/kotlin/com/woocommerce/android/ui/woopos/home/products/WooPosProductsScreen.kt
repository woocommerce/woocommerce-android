package com.woocommerce.android.ui.woopos.home.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.EndOfProductListReached
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.ItemClicked
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.ProductsLoadingErrorRetryButtonClicked
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.PullToRefreshTriggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosProductsScreen(modifier: Modifier = Modifier) {
    val productsViewModel: WooPosProductsViewModel = hiltViewModel()
    WooPosProductsScreen(
        modifier = modifier,
        productsStateFlow = productsViewModel.viewState,
        onItemClicked = { productsViewModel.onUIEvent(ItemClicked(it)) },
        onEndOfProductListReached = { productsViewModel.onUIEvent(EndOfProductListReached) },
        onPullToRefresh = { productsViewModel.onUIEvent(PullToRefreshTriggered) },
        onRetryClicked = { productsViewModel.onUIEvent(ProductsLoadingErrorRetryButtonClicked) },
        onSimpleProductsBannerClosed = {
            productsViewModel.onUIEvent(WooPosProductsUIEvent.SimpleProductsBannerClosed)
        },
        onSimpleProductsBannerLearnMoreClicked = {
            productsViewModel.onUIEvent(WooPosProductsUIEvent.SimpleProductsBannerLearnMoreClicked)
        },
        onToolbarInfoIconClicked = {
            productsViewModel.onUIEvent(WooPosProductsUIEvent.SimpleProductsDialogInfoIconClicked)
        },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosProductsScreen(
    modifier: Modifier = Modifier,
    productsStateFlow: StateFlow<WooPosProductsViewState>,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductListReached: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRetryClicked: () -> Unit,
    onSimpleProductsBannerClosed: () -> Unit,
    onSimpleProductsBannerLearnMoreClicked: () -> Unit,
    onToolbarInfoIconClicked: () -> Unit,
) {
    val state = productsStateFlow.collectAsState()
    val pullToRefreshState = rememberPullRefreshState(state.value.reloadingProductsWithPullToRefresh, onPullToRefresh)
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
                is WooPosProductsViewState.Loading,
                is WooPosProductsViewState.Empty,
                is WooPosProductsViewState.Error -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

                is WooPosProductsViewState.Content -> MaterialTheme.colors.onSurface
            }
            ProductsToolbar(state.value, modifier, titleColor, onToolbarInfoIconClicked)

            Spacer(modifier = Modifier.height(24.dp))

            when (val productsState = state.value) {
                is WooPosProductsViewState.Content -> {
                    Column {
                        SimpleProductsBanner(
                            productsState.bannerState,
                            onSimpleProductsBannerLearnMoreClicked,
                            onSimpleProductsBannerClosed
                        )
                        ProductsList(
                            productsState,
                            onItemClicked,
                            onEndOfProductListReached,
                        )
                    }
                }

                is WooPosProductsViewState.Loading -> ProductsLoadingIndicator()

                is WooPosProductsViewState.Empty -> ProductsEmptyList()

                is WooPosProductsViewState.Error -> ProductsError { onRetryClicked() }
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
private fun ProductsToolbar(
    productViewState: WooPosProductsViewState,
    modifier: Modifier,
    titleColor: Color,
    onToolbarInfoIconClicked: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.woopos_products_screen_title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        when (productViewState) {
            is WooPosProductsViewState.Content -> {
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
                            tint = MaterialTheme.colors.primary,
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
    bannerState: WooPosProductsViewState.Content.BannerState,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductsList(
    state: WooPosProductsViewState.Content,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductsListReached: () -> Unit,
) {
    val listState = rememberLazyListState()
    WooPosLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
        state = listState,
    ) {
        items(
            state.products,
            key = { product -> product.id }
        ) { product ->
            ProductItem(
                modifier = Modifier.animateItemPlacement(),
                item = product,
                onItemClicked = onItemClicked
            )
        }

        if (state.loadingMore) {
            item {
                ProductLoadingItem()
            }
        }
        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
    InfiniteListHandler(listState) {
        onEndOfProductsListReached()
    }
}

@Composable
fun ProductsLoadingIndicator() {
    WooPosLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
    ) {
        items(10) {
            ProductLoadingItem()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductLoadingItem() {
    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(WooPosTheme.colors.loadingSkeleton)
            )

            Spacer(modifier = Modifier.width(32.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(184.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(30.dp)
                    .width(76.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: WooPosProductsListItem,
    onItemClicked: (item: WooPosProductsListItem) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier
                .clickable { onItemClicked(item) }
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                fallback = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                error = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                placeholder = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                contentDescription = stringResource(id = R.string.woopos_product_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(112.dp)
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = item.name,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                text = item.price,
                style = MaterialTheme.typography.h5,
            )

            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
fun ProductsEmptyList() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(104.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.woo_pos_ic_empty_products),
                contentDescription = stringResource(id = R.string.woopos_products_empty_list_image_description),
            )

            Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))

            Text(
                text = stringResource(id = R.string.woopos_products_empty_list_title),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

            Text(
                text = stringResource(id = R.string.woopos_products_empty_list_message),
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp.toAdaptivePadding()))
        }
    }
}

@Composable
fun ProductsError(onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorState(
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
private fun InfiniteListHandler(
    listState: LazyListState,
    onEndOfProductsListReached: () -> Unit
) {
    val buffer = 5
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onEndOfProductsListReached()
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosProductsScreenPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            products = listOf(
                WooPosProductsListItem(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            loadingMore = true,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosProductsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            modifier = modifier,
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
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
fun WooPosProductsScreenLoadingPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Loading(
            reloadingProductsWithPullToRefresh = true,
            withCart = false
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
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
    val productState = MutableStateFlow(WooPosProductsViewState.Empty(true))
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
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
    val productState = MutableStateFlow(WooPosProductsViewState.Error())
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
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
fun WooPosHomeScreenProductsWithSimpleProductsOnlyBannerPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            products = listOf(
                WooPosProductsListItem(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            loadingMore = false,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosProductsViewState.Content.BannerState(
                isBannerHiddenByUser = false,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            )
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
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
fun WooPosHomeScreenProductsWithInfoIconInToolbarPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState.Content(
            products = listOf(
                WooPosProductsListItem(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            loadingMore = false,
            reloadingProductsWithPullToRefresh = false,
            bannerState = WooPosProductsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            )
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            onSimpleProductsBannerClosed = {},
            onSimpleProductsBannerLearnMoreClicked = {},
            onToolbarInfoIconClicked = {},
        )
    }
}
