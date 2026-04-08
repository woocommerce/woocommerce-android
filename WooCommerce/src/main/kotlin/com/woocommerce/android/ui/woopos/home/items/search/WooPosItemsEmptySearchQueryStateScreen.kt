package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.SectionHeader
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosRecentSearchesChips
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductCard

@Composable
fun WooPosItemsEmptySearchQueryStateScreen(
    modifier: Modifier = Modifier,
    state: WooPosItemsSearchViewState.EmptySearchQuery,
    onUIEvent: (WooPosItemsSearchUiEvent) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }
    Column(
        modifier
            .fillMaxHeight()
            .padding(top = WooPosSpacing.Small.value)
            .padding(top = WooPosSpacing.XSmall.value)
            .verticalScroll(scrollState)
    ) {
        if (state.recentSearches.isNotEmpty()) {
            WooPosRecentSearchesChips(
                recentSearches = state.recentSearches,
                onRecentSearchClicked = { recentSearch ->
                    onUIEvent(WooPosItemsSearchUiEvent.OnRecentSearchClicked(recentSearch))
                }
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }

        if (state.popularItems.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WooPosSpacing.Medium.value),
            ) {
                PopularItemsSection(
                    popularItems = state.popularItems,
                    onPopularItemClicked = { popularItem ->
                        onUIEvent(WooPosItemsSearchUiEvent.OnPopularItemClicked(popularItem))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Gigantic.value))
    }
}

@Composable
private fun PopularItemsSection(
    popularItems: List<WooPosItemSelectionViewState.Product>,
    onPopularItemClicked: (WooPosItemSelectionViewState.Product) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.woopos_search_popular_items_title)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        popularItems.forEach { popularItem ->
            val itemContentDescription = stringResource(
                id = R.string.woopos_product_item_content_description,
                popularItem.name,
                popularItem.price
            )

            WooPosProductCard(
                modifier = Modifier,
                itemContentDescription = itemContentDescription,
                onItemClicked = { onPopularItemClicked(popularItem) },
                item = popularItem,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }
    }
}

@WooPosPreview
@Composable
fun WooPosItemsEmptySearchQueryStatePreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsEmptySearchQueryStateScreen(
                state = WooPosItemsSearchViewState.EmptySearchQuery(
                    popularItems = listOf(
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 1,
                            name = "Popular Item 1",
                            price = "10.0$",
                            imageUrl = "https://example.com/image1.jpg",
                        ),
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 2,
                            name = "Popular Item 2",
                            price = "20.0$",
                            imageUrl = "https://example.com/image2.jpg",
                        ),
                        WooPosItemSelectionViewState.Product.Variable(
                            id = 3,
                            name = "Popular Item 3",
                            price = "30.0$",
                            imageUrl = "https://example.com/image3.jpg",
                            numOfVariations = 3,
                            variationIds = listOf(1, 2, 3),
                        ),
                    ),
                    recentSearches = listOf("Chocolate", "Mug", "Hario", "Coffee"),
                ),
                onUIEvent = { },
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosItemsEmptySearchQueryStateOnyItemsPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosItemsEmptySearchQueryStateScreen(
                state = WooPosItemsSearchViewState.EmptySearchQuery(
                    popularItems = listOf(
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 1,
                            name = "Popular Item 1",
                            price = "10.0$",
                            imageUrl = "https://example.com/image1.jpg",
                        ),
                        WooPosItemSelectionViewState.Product.Simple(
                            id = 2,
                            name = "Popular Item 2",
                            price = "20.0$",
                            imageUrl = "https://example.com/image2.jpg",
                        ),
                        WooPosItemSelectionViewState.Product.Variable(
                            id = 3,
                            name = "Popular Item 3",
                            price = "30.0$",
                            imageUrl = "https://example.com/image3.jpg",
                            numOfVariations = 3,
                            variationIds = listOf(1, 2, 3),
                        ),
                    ),
                    recentSearches = emptyList()
                ),
                onUIEvent = { },
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosRecentSearchesChipsPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosRecentSearchesChips(
                recentSearches = listOf("Chocolate", "Mug", "Hario", "Coffee", "Prezzetti"),
                onRecentSearchClicked = {}
            )
        }
    }
}
