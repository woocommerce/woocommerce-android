package com.woocommerce.android.ui.woopos.home.items.search

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemCard
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState

@Composable
fun WooPosItemsEmptySearchQueryState(state: WooPosItemsSearchViewState.EmptySearchQuery) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(
                start = WooPosSpacing.None.value.toAdaptivePadding(),
                end = WooPosSpacing.None.value.toAdaptivePadding(),
                top = WooPosSpacing.Small.value.toAdaptivePadding(),
                bottom = WooPosSpacing.None.value.toAdaptivePadding(),
            )
    ) {
        if (state.popularItems.isNotEmpty() || state.recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.popularItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        PopularItemsSection(state.popularItems)
                    }
                }

                if (state.popularItems.isNotEmpty() && state.recentSearches.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
                }

                if (state.recentSearches.isNotEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        RecentSearchesSection(state)
                    }
                }
            }
        }

        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun PopularItemsSection(popularItems: List<WooPosItemSelectionViewState.Product>) {
    SectionHeader(
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        title = stringResource(R.string.woopos_search_popular_items_title)
    )

    Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

    popularItems.forEach { popularItem ->
        val itemContentDescription = stringResource(
            id = R.string.woopos_product_item_content_description,
            popularItem.name,
            popularItem.price
        )

        WooPosItemCard(
            modifier = Modifier,
            itemContentDescription = itemContentDescription,
            onItemClicked = { },
            item = popularItem,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun RecentSearchesSection(state: WooPosItemsSearchViewState.EmptySearchQuery) {
    SectionHeader(
        icon = Icons.Filled.History,
        title = stringResource(R.string.woopos_search_recent_searches_title)
    )

    Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

    state.recentSearches.forEach { recentSearch ->
        WooPosCard(
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowType = ShadowType.Soft,
        ) {
            Row(
                modifier = Modifier
                    .clickable { }
                    .height(112.dp)
                    .fillMaxWidth()
                    .padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(WooPosSpacing.Small.value.toAdaptivePadding()))
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                )

                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

                WooPosText(
                    text = recentSearch,
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value.toAdaptivePadding()))

        WooPosText(
            text = title,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
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
            WooPosItemsEmptySearchQueryState(
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
                    recentSearches = listOf("T-shirt", "Jeans", "Shoes"),
                )
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
            WooPosItemsEmptySearchQueryState(
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
                )
            )
        }
    }
}
