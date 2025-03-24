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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.EndOfItemsListReached
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.ProductsLoadingErrorRetryButtonClicked
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchAnimationCompleted
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchChanged
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    // CouponsProject: Needs to be renamed to WooPosItemsViewModel
    val productsViewModel: WooPosItemsViewModel = hiltViewModel()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        listState = listState,
        onUIEvent = { productsViewModel.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosItemsViewState>,
    listState: LazyListState,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
) {
    val state = itemsStateFlow.collectAsState()
    val pullToRefreshState = rememberPullRefreshState(
        state.value.reloadingProductsWithPullToRefresh,
        onRefresh = { onUIEvent(PullToRefreshTriggered) },
    )

    MainItemsList(
        modifier = modifier,
        pullToRefreshState = pullToRefreshState,
        state = state,
        listState = listState,
        onToolbarInfoIconClicked = {
            onUIEvent(WooPosItemsUIEvent.SimpleProductsDialogInfoIconClicked)
        },
        onSimpleProductsBannerLearnMoreClicked = {
            onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerLearnMoreClicked)
        },
        onSimpleProductsBannerClosed = {
            onUIEvent(WooPosItemsUIEvent.SimpleProductsBannerClosed)
        },
        onItemClicked = { item ->
            onUIEvent(WooPosItemsUIEvent.ItemClicked(item))
        },
        onEndOfItemListReached = { onUIEvent(EndOfItemsListReached) },
        onRetryClicked = { onUIEvent(ProductsLoadingErrorRetryButtonClicked) },
        onSearchEvent = {
            when (it) {
                WooPosSearchUIEvent.Clear -> onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)
                WooPosSearchUIEvent.Close -> onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)
                is WooPosSearchUIEvent.Search -> onUIEvent(SearchChanged(it.query))
                WooPosSearchUIEvent.AnimationComplete -> onUIEvent(SearchAnimationCompleted)
            }
        },
        onCouponsButtonClicked = { onUIEvent(WooPosItemsUIEvent.CouponsButtonClicked) },
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
    onRetryClicked: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onCouponsButtonClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullToRefreshState)
            .padding(
                start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                top = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                bottom = WooPosSpacing.None.value.toAdaptivePadding(),
            )
    ) {
        Column(
            modifier.fillMaxHeight()
        ) {
            val titleColor = when (state.value) {
                is WooPosItemsViewState.Loading,
                is WooPosItemsViewState.Empty,
                is WooPosItemsViewState.Error -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                is WooPosItemsViewState.Content -> MaterialTheme.colorScheme.onSurface
            }
            ItemsToolbar(state.value, titleColor, onToolbarInfoIconClicked, onCouponsButtonClicked)

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            when (val itemsState = state.value) {
                is WooPosItemsViewState.Content -> {
                    Column {
                        SimpleProductsBanner(
                            itemsState.bannerState,
                            onSimpleProductsBannerLearnMoreClicked,
                            onSimpleProductsBannerClosed
                        )

                        when (itemsState.search) {
                            is WooPosItemsViewState.Content.SearchState.Visible -> {
                                WooPosSearchInput(
                                    state = itemsState.search.state,
                                    onEvent = onSearchEvent,
                                )
                                when (itemsState.search.state) {
                                    WooPosSearchInputState.Closed -> {
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
                                    is WooPosSearchInputState.Open -> WooPosItemsSearchScreen()
                                }
                            }

                            WooPosItemsViewState.Content.SearchState.Hidden -> {
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
    onCouponsButtonClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        WooPosText(
            text = stringResource(id = R.string.woopos_products_screen_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        when (productViewState) {
            is WooPosItemsViewState.Content -> {
                if (productViewState.couponsEnabled) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            onCouponsButtonClicked()
                        }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_more_menu_coupons),
                            contentDescription = stringResource(
                                id = R.string.coupons
                            ),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                        )
                    }
                }
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
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                        )
                    }
                }
            }
            else -> {
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
            paginationState = PaginationState.Loading,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query(""),
                    isLoading = false,
                )
            )
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
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
        WooPosItemsViewState.Content(
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
            paginationState = PaginationState.Error,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
            search = WooPosItemsViewState.Content.SearchState.Hidden,
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
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
        WooPosItemsViewState.Loading(
            reloadingProductsWithPullToRefresh = true,
            withCart = false
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
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
    val productState = MutableStateFlow(WooPosItemsViewState.Empty(true))
    WooPosTheme {
        WooPosItemsScreen(
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
    val productState = MutableStateFlow(WooPosItemsViewState.Error())
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
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
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = false,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query(""),
                    isLoading = false,
                )
            )
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
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
        WooPosItemsViewState.Content(
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
            reloadingProductsWithPullToRefresh = false,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
            search = WooPosItemsViewState.Content.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query(""),
                    isLoading = false,
                )
            )
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            itemsStateFlow = productState,
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}
