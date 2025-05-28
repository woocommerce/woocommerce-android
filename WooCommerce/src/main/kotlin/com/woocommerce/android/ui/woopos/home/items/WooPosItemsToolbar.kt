package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularIconButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant

val WOO_POS_ITEMS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosItemsToolbar(
    modifier: Modifier = Modifier,
    state: WooPosItemsToolbarViewState,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onBackClicked: () -> Unit,
    onAddCouponEvent: () -> Unit,
) {
    val isSearchOpen = (state.search as? SearchState.Visible)?.let {
        it.state is WooPosSearchInputState.Open
    } == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT)
    ) {
        when {
            isSearchOpen -> {
                WooPosSearchInput(
                    state = (state.search as SearchState.Visible).state,
                    onEvent = { event ->
                        onSearchEvent(event)
                    },
                )
            }

            state.backNavigation -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value.toAdaptivePadding()))
                    IconButton(
                        onClick = { onBackClicked() },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.woopos_toolbar_icon_content_description),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value.toAdaptivePadding()))

                    TabsRow(
                        tabs = state.tabs,
                        onTabClicked = onTabClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

                    TabsRow(
                        tabs = state.tabs,
                        onTabClicked = onTabClicked,
                        modifier = Modifier.weight(1f)
                    )

                    if (state is WooPosItemsToolbarViewState.CouponList) {
                        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                        WooPosCircularIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = stringResource(
                                id = R.string.woopos_coupons_empty_list_create_coupon_label,
                            ),
                            onClick = { onAddCouponEvent() }
                        )
                    }

                    when (val search = state.search) {
                        SearchState.Hidden -> Unit
                        is SearchState.Visible -> {
                            WooPosSearchInput(
                                state = search.state,
                                onEvent = { event ->
                                    onSearchEvent(event)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabsRow(
    tabs: List<WooPosItemsToolbarViewState.Tab>,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
    ) {
        items(tabs.size) { index ->
            val tab = tabs[index]
            WooPosText(
                text = tab.name,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = tab.highlightLevel.titleColor(),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTabClicked(tab) }
                )
            )
            Spacer(modifier = Modifier.width(WooPosSpacing.Large.value))
        }
    }
}

@Composable
private fun WooPosItemsToolbarViewState.Tab.HighlightLevel.titleColor(): Color = when (this) {
    WooPosItemsToolbarViewState.Tab.HighlightLevel.Full -> MaterialTheme.colorScheme.onSurface
    WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal -> WooPosTheme.colors.onSurfaceVariantLowest
}

@Composable
@WooPosPreview
fun WooPosProductsToolbarPreview() {
    val tabs = listOf(
        WooPosItemsToolbarViewState.Tab.Products(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.Coupons(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.ProductList(
                tabs = tabs,
                search = SearchState.Visible(WooPosSearchInputState.Closed)
            ),
            onTabClicked = {},
            onSearchEvent = {},
            onAddCouponEvent = {},
            onBackClicked = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosCouponsToolbarPreview() {
    val tabs = listOf(
        WooPosItemsToolbarViewState.Tab.Products(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.Coupons(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.CouponList(
                tabs = tabs,
                search = SearchState.Visible(WooPosSearchInputState.Closed)
            ),
            onTabClicked = {},
            onSearchEvent = {},
            onAddCouponEvent = {},
            onBackClicked = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosItemsToolbarWithSearchPreview() {
    val tabs = listOf(
        WooPosItemsToolbarViewState.Tab.Products(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.Coupons(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.ProductList(
                tabs = tabs,
                search = SearchState.Visible(
                    state = WooPosSearchInputState.Open(
                        input = WooPosSearchInputState.Open.Input.Query(
                            query = "",
                            cursorPosition = 1,
                        ),
                        isLoading = false,
                    )
                )
            ),
            onTabClicked = {},
            onSearchEvent = {},
            onAddCouponEvent = {},
            onBackClicked = {},
        )
    }
}

@Composable
@WooPosPreview
fun WooPosItemsToolbarWithVariationsPreview() {
    val tabs = listOf(
        WooPosItemsToolbarViewState.Tab.Variations(
            name = "Variations",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.VariationList(
                tabs = tabs,
                variableProductData = WooPosVariationsNavigationData(
                    id = 1L,
                    numOfVariations = 2,
                    sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST,
                )
            ),
            onTabClicked = {},
            onSearchEvent = {},
            onAddCouponEvent = {},
            onBackClicked = {},
        )
    }
}
