package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Composable
fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<WooPosItemsSearchViewModel>()
    val state = viewModel.viewState.collectAsState().value
    WooPosItemsSearchScreen(
        modifier = modifier.imePadding(),
        state = state,
        onUIEvent = viewModel::onUIEvent,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
    state: WooPosItemsSearchViewState,
    onUIEvent: (WooPosItemsSearchUiEvent) -> Unit = {},
) {
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
        onRefresh = { onUIEvent(WooPosItemsSearchUiEvent.OnPullToRefreshTriggered) },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(
                state = pullRefreshState,
                enabled = state.pullToRefreshState == WooPosPullToRefreshState.Enabled,
            ),
    ) {
        val stateClass by remember(state) {
            derivedStateOf {
                state.javaClass
            }
        }

        if (state is WooPosItemsSearchViewState.Content) {
            LaunchedEffect(state.searchQuery) {
                listState.scrollToItem(0)
            }
        }

        Crossfade(
            targetState = stateClass,
            label = "WooPosItemsSearchScreenCrossfade"
        ) { currentStateClass ->
            when (currentStateClass) {
                WooPosItemsSearchViewState.EmptySearchQuery::class.java -> {
                    if (state is WooPosItemsSearchViewState.EmptySearchQuery) {
                        WooPosItemsEmptySearchQueryStateScreen(
                            modifier = Modifier.imePadding(),
                            state = state,
                            onUIEvent = onUIEvent
                        )
                    }
                }

                WooPosItemsSearchViewState.Content::class.java -> {
                    if (state is WooPosItemsSearchViewState.Content) {
                        WooPosItemsSearchContent(
                            modifier = Modifier.padding(
                                horizontal = WooPosSpacing.Medium.value,
                            ),
                            listState = listState,
                            state = state,
                            onUIEvent = onUIEvent
                        )
                    }
                }

                WooPosItemsSearchViewState.Empty::class.java -> {
                    WooPosEmptyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = WooPosSpacing.Medium.value,
                            )
                            .imePadding(),
                        title = stringResource(id = R.string.woopos_search_items_empty_title),
                        message = stringResource(id = R.string.woopos_search_empty_description),
                        contentDescription = stringResource(
                            id = R.string.woopos_search_empty_image_content_description
                        ),
                    )
                }

                WooPosItemsSearchViewState.Error::class.java -> {
                    if (state is WooPosItemsSearchViewState.Error) {
                        WooPosErrorScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = WooPosSpacing.Medium.value)
                                .verticalScroll(rememberScrollState())
                                .imePadding(),
                            message = stringResource(id = R.string.woopos_search_items_error_title),
                            reason = stringResource(id = R.string.woopos_search_items_error_description),
                            primaryButton = WooPosErrorScreenButtonState(
                                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                                click = { onUIEvent(WooPosItemsSearchUiEvent.LoadingErrorRetryButtonClicked) }
                            )
                        )
                    }
                }

                WooPosItemsSearchViewState.Loading::class.java -> {
                    WooPosItemsLoadingIndicator(
                        modifier = Modifier
                            .padding(
                                start = WooPosSpacing.Medium.value,
                                end = WooPosSpacing.Medium.value,
                                top = WooPosSpacing.Small.value
                            )
                            .padding(top = WooPosSpacing.XSmall.value),
                        itemsCount = 5
                    )
                }

                else -> error("Unsupported state: $currentStateClass")
            }
        }

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            state = pullRefreshState
        )
    }
}

@Composable
private fun WooPosItemsSearchContent(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    state: WooPosItemsSearchViewState.Content,
    onUIEvent: (WooPosItemsSearchUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }
    WooPosItemList(
        modifier = modifier.padding(top = WooPosSpacing.XSmall.value),
        state = state,
        listState = listState,
        onItemClicked = { onUIEvent(WooPosItemsSearchUiEvent.OnItemClicked(it)) },
        onEndOfProductsListReached = { onUIEvent(WooPosItemsSearchUiEvent.OnNextPageRequested) },
        onErrorWhilePaginating = {
            WooPosPaginationErrorIndicator(
                message = stringResource(id = R.string.woopos_items_pagination_error_title),
                description = stringResource(id = R.string.woopos_items_pagination_error_description),
                primaryButton = WooPosErrorScreenButtonState(
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
            WooPosItemsEmptySearchQueryStateScreen(
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
                ),
                onUIEvent = { }
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
                    pullToRefreshState = WooPosPullToRefreshState.Enabled,
                    paginationState = WooPosPaginationState.None
                ),
                listState = rememberLazyListState(),
                onUIEvent = {},
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
                state = WooPosItemsSearchViewState.Empty(),
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
