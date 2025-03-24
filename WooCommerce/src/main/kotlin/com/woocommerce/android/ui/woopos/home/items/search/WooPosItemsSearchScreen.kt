package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItem

@Composable
fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<WooPosItemsSearchViewModel>()
    val state = viewModel.viewState.collectAsState().value
    WooPosItemsSearchScreen(
        modifier = modifier,
        state = state,
    )
}

@Composable
private fun WooPosItemsSearchScreen(
    modifier: Modifier = Modifier,
    state: WooPosItemsSearchViewState,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (state) {
            is WooPosItemsSearchViewState.EmptySearchQuery -> {
                WooPosItemsEmptySearchQueryState(state)
            }

            is WooPosItemsSearchViewState.Content -> {
            }

            WooPosItemsSearchViewState.Empty -> {
            }
        }
    }
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
                    popularItems = listOf<WooPosItem.Product>(
                        WooPosItem.Product.Simple(
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
