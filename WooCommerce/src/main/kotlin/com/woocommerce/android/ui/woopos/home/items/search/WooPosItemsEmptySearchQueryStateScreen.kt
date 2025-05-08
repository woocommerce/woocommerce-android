package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosChip
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
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
            .padding(top = WooPosSpacing.Large.value.toAdaptivePadding())
            .verticalScroll(scrollState)
    ) {
        if (state.recentSearches.isNotEmpty()) {
            RecentSearchesChips(
                recentSearches = state.recentSearches,
                onRecentSearchClicked = { recentSearch ->
                    onUIEvent(WooPosItemsSearchUiEvent.OnRecentSearchClicked(recentSearch))
                }
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }

        if (state.popularItems.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PopularItemsSection(
                    popularItems = state.popularItems,
                    onPopularItemClicked = { popularItem ->
                        onUIEvent(WooPosItemsSearchUiEvent.OnPopularItemClicked(popularItem))
                    }
                )
            }
        }

        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun RecentSearchesChips(
    recentSearches: List<String>,
    onRecentSearchClicked: (String) -> Unit,
) {
    Column {
        SectionHeader(
            title = stringResource(R.string.woopos_search_recent_searches_title)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        val horizontalScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            recentSearches.forEach { recentSearch ->
                WooPosChip(
                    text = recentSearch,
                    onClick = { onRecentSearchClicked(recentSearch) },
                    leadingIcon = Icons.Default.Search
                )
            }
        }
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

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

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

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    WooPosText(
        text = title,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
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
            RecentSearchesChips(
                recentSearches = listOf("Chocolate", "Mug", "Hario", "Coffee", "Prezzetti"),
                onRecentSearchClicked = {}
            )
        }
    }
}
