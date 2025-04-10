package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.EndOfItemsListReached
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.ProductsLoadingErrorRetryButtonClicked
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.PullToRefreshTriggered
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

    MainItemsList(
        modifier = modifier,
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
                is WooPosSearchUIEvent.AnimationComplete -> {
                    onUIEvent(WooPosItemsUIEvent.SearchAnimationComplete)
                }
            }
        },
        onCouponsButtonClicked = { onUIEvent(WooPosItemsUIEvent.CouponsButtonClicked) },
        onPullToRefreshTriggered = { onUIEvent(PullToRefreshTriggered) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    state: State<WooPosItemsViewState>,
    listState: LazyListState,
    onToolbarInfoIconClicked: () -> Unit,
    onSimpleProductsBannerLearnMoreClicked: () -> Unit,
    onSimpleProductsBannerClosed: () -> Unit,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onRetryClicked: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onCouponsButtonClicked: () -> Unit,
    onPullToRefreshTriggered: () -> Unit,
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
            ItemsToolbar(
                state = state.value,
                titleColor = titleColor,
                onToolbarInfoIconClicked = onToolbarInfoIconClicked,
                onCouponsButtonClicked = onCouponsButtonClicked,
                onSearchEvent = onSearchEvent,
            )

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

                is WooPosItemsViewState.Loading -> WooPosItemsLoadingIndicator()

                is WooPosItemsViewState.Empty -> WooPosItemsEmptyList(
                    title = stringResource(id = R.string.woopos_products_empty_list_title),
                    message = stringResource(id = R.string.woopos_products_empty_list_message),
                    contentDescription = stringResource(id = R.string.woopos_products_empty_list_image_description),
                )

                is WooPosItemsViewState.Error -> ProductsError { onRetryClicked() }
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
private fun ItemsToolbar(
    state: WooPosItemsViewState,
    titleColor: Color,
    onToolbarInfoIconClicked: () -> Unit,
    onCouponsButtonClicked: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
) {
    val isSearchExpanded = state is WooPosItemsViewState.Content &&
        state.search is WooPosItemsViewState.Content.SearchState.Visible &&
        state.search.state is WooPosSearchInputState.Open

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedVisibility(
            visible = !isSearchExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            WooPosText(
                text = stringResource(id = R.string.woopos_products_screen_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state) {
                is WooPosItemsViewState.Content -> {
                    when (val searchState = state.search) {
                        WooPosItemsViewState.Content.SearchState.Hidden -> Unit
                        is WooPosItemsViewState.Content.SearchState.Visible -> {
                            WooPosSearchInput(
                                state = searchState.state,
                                onEvent = onSearchEvent,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))

                            VerticalDivider(
                                modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
                                thickness = 1.dp,
                            )

                            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
                        }
                    }

                    if (state.couponsEnabled) {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                onCouponsButtonClicked()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = stringResource(
                                    id = R.string.coupons
                                ),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))

                        VerticalDivider(
                            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
                            thickness = 1.dp,
                        )

                        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
                    }

                    if (state.bannerState.isBannerHiddenByUser) {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                onToolbarInfoIconClicked()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(
                                    id = R.string.woopos_banner_simple_products_info_content_description
                                ),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                is WooPosItemsViewState.Empty,
                is WooPosItemsViewState.Error,
                is WooPosItemsViewState.Loading -> Unit
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
            paginationState = WooPosPaginationState.Loading,
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
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
            paginationState = WooPosPaginationState.Error,
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
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
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
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
    val productState = MutableStateFlow(
        WooPosItemsViewState.Empty(
            WooPosPullToRefreshState.Refreshing,
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
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
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
            pullToRefreshState = WooPosPullToRefreshState.Disabled,
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
