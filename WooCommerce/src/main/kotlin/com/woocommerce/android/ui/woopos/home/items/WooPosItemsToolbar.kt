package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularIconButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
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
    leadingContent: (@Composable () -> Unit)? = null,
    showAddCouponButton: Boolean = true,
) {
    val isSearchOpen = (state.search as? SearchState.Visible)?.let {
        it.state is WooPosSearchInputState.Open
    } == true

    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    val tabsTextStyle = if (isPhone) WooPosTypography.BodyLarge else WooPosTypography.Heading

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT)
    ) {
        when (isSearchOpen) {
            true -> WooPosSearchInput(
                state = (state.search as SearchState.Visible).state,
                onEvent = onSearchEvent,
            )
            false -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    leadingContent != null -> leadingContent()
                    state.backNavigation -> {
                        Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                        WooPosBackButton { onBackClicked() }
                        Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                    }
                    else -> Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                }

                WooPosItemsTabsRow(
                    tabs = state.tabs,
                    onTabClicked = onTabClicked,
                    itemSpacing = WooPosSpacing.Large.value,
                    textStyle = tabsTextStyle,
                    modifier = Modifier.weight(1f),
                )

                if (showAddCouponButton && state is WooPosItemsToolbarViewState.CouponList) {
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                    WooPosCircularIconButton(
                        icon = ImageVector.vectorResource(R.drawable.ic_add),
                        contentDescription = stringResource(
                            id = R.string.woopos_coupons_empty_list_create_coupon_label,
                        ),
                        onClick = { onAddCouponEvent() }
                    )
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                }

                when (val search = state.search) {
                    SearchState.Hidden -> Unit
                    is SearchState.Visible -> {
                        WooPosSearchInput(
                            state = search.state,
                            onEvent = onSearchEvent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WooPosItemsTabsRow(
    tabs: List<WooPosItemsToolbarViewState.Tab>,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    itemSpacing: Dp,
    modifier: Modifier = Modifier,
    textStyle: WooPosTypography = WooPosTypography.Heading,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            WooPosText(
                text = tab.name,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = tab.highlightLevel.titleColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabClicked(tab) }
                    )
            )
            if (index < tabs.lastIndex) {
                Spacer(modifier = Modifier.width(itemSpacing))
            }
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
