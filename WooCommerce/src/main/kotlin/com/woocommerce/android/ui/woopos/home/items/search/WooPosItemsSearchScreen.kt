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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

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
            }

            WooPosItemsSearchViewState.Error -> {
            }

            WooPosItemsSearchViewState.Loading -> {
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
