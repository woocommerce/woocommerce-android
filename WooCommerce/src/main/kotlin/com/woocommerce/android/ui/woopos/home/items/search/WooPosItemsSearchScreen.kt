package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsEmptyList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator

@Composable
fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<WooPosItemsSearchViewModel>()
    val state = viewModel.viewState.collectAsState().value
    WooPosItemsSearchScreen(
        modifier = modifier,
        state = state,
        onUIEvent = viewModel::onUIEvent,
    )
}

@Composable
private fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
    state: WooPosItemsSearchViewState,
    onUIEvent: (WooPosItemsSearchUiEvent) -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (state) {
            is WooPosItemsSearchViewState.EmptySearchQuery -> {
                WooPosItemsEmptySearchQueryState(state)
            }

            is WooPosItemsSearchViewState.Content -> {
                WooPosItemsSearchContent(state, onUIEvent)
            }

            WooPosItemsSearchViewState.Empty -> {
                WooPosItemsEmptyList(
                    title = stringResource(id = R.string.woopos_search_items_empty_title),
                    message = stringResource(id = R.string.woopos_search_empty_description),
                    contentDescription = stringResource(id = R.string.woopos_search_empty_image_content_description),
                )
            }

            is WooPosItemsSearchViewState.Error -> {
                WooPosErrorScreen(
                    message = stringResource(id = R.string.woopos_search_items_error_title),
                    reason = stringResource(id = R.string.woopos_search_items_error_description),
                    primaryButton = Button(
                        text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                        click = { onUIEvent(WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked) }
                    )
                )
            }

            WooPosItemsSearchViewState.Loading -> {
                WooPosItemsLoadingIndicator(itemsCount = 5)
            }
        }
    }
}

@Composable
private fun WooPosItemsSearchContent(
    state: WooPosItemsSearchViewState.Content,
    onUIEvent: (WooPosItemsSearchUiEvent) -> Unit
) {
    val listState = rememberLazyListState()
    WooPosItemList(
        state = state,
        listState = listState,
        onItemClicked = { onUIEvent(WooPosItemsSearchUiEvent.ItemClicked(it)) },
        onEndOfProductsListReached = { onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested) },
        onErrorWhilePaginating = {
            WooPosPaginationErrorIndicator(
                message = stringResource(id = R.string.woopos_items_pagination_error_title),
                description = stringResource(id = R.string.woopos_items_pagination_error_description),
                primaryButton = Button(
                    text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
                    click = { onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested) }
                ),
            )
        }
    )
}

@Composable
@WooPosPreview
fun WooPosItemsSearchScreenPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsEmptySearchQueryState(
                state = WooPosItemsSearchViewState.EmptySearchQuery(
                    popularItems = listOf<WooPosItemSelectionViewState.Product>(
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 1,
                            name = "Popular Item 1",
                            price = "10.0$",
                            imageUrl = "https://example.com/image1.jpg",
                        ),
                    ),
                    recentSearches = listOf(
                        "Recent Search 1",
                        "Recent Search 2",
                        "Recent Search 3",
                    )
                )
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosItemsSearchContentPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsSearchContent(
                state = WooPosItemsSearchViewState.Content(
                    searchQuery = "item",
                    items = listOf(
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 1,
                            name = "Item 1",
                            price = "10.0$",
                            imageUrl = "https://example.com/image1.jpg",
                        ),
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 2,
                            name = "Item 2",
                            price = "20.0$",
                            imageUrl = "https://example.com/image2.jpg",
                        ),
                    ),
                    reloadingWithPullToRefresh = false,
                    paginationState = PaginationState.None
                ),
                onUIEvent = {}
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosItemsSearchEmptyPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsSearchScreen(
                state = WooPosItemsSearchViewState.Empty,
                onUIEvent = {}
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosItemsSearchErrorPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsSearchScreen(
                state = WooPosItemsSearchViewState.Error(searchQuery = ""),
                onUIEvent = {}
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosItemsSearchLoadingPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsSearchScreen(
                state = WooPosItemsSearchViewState.Loading,
                onUIEvent = {}
            )
        }
    }
}
