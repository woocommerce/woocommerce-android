package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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

private const val ANIMATION_DURATION = 300
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

    val layoutState = when {
        isSearchOpen -> ToolbarLayoutState.SearchOpen(
            search = state.search as SearchState.Visible
        )
        state.backNavigation -> ToolbarLayoutState.TabsWithBackButton(
            tabs = state.tabs,
            search = state.search,
            showAddCouponButton = state is WooPosItemsToolbarViewState.CouponList,
        )
        else -> ToolbarLayoutState.NormalTabs(
            tabs = state.tabs,
            search = state.search,
            showAddCouponButton = state is WooPosItemsToolbarViewState.CouponList,
        )
    }

    Crossfade(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WOO_POS_ITEMS_TOOLBAR_HEIGHT),
        targetState = layoutState,
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = LinearEasing,
        )
    ) { currentLayoutState ->
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (currentLayoutState) {
                is ToolbarLayoutState.SearchOpen -> {
                    Box {
                        WooPosSearchInput(
                            state = currentLayoutState.search.state,
                            animationDuration = ANIMATION_DURATION,
                            onEvent = { event ->
                                onSearchEvent(event)
                            },
                        )
                    }
                }

                is ToolbarLayoutState.TabsWithBackButton -> {
                    Row(
                        modifier = Modifier
                            .weight(1f),
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

                        LazyRow(
                            modifier = Modifier.weight(1f),
                        ) {
                            items(currentLayoutState.tabs.size) { index ->
                                val tab = currentLayoutState.tabs[index]
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

                        if (currentLayoutState.showAddCouponButton) {
                            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                            WooPosCircularIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = stringResource(
                                    id = R.string.woopos_coupons_empty_list_create_coupon_label,
                                ),
                                onClick = { onAddCouponEvent() }
                            )
                        }
                    }

                    when (val search = currentLayoutState.search) {
                        SearchState.Hidden -> Unit
                        is SearchState.Visible -> {
                            Box {
                                WooPosSearchInput(
                                    state = search.state,
                                    animationDuration = ANIMATION_DURATION,
                                    onEvent = { event ->
                                        onSearchEvent(event)
                                    },
                                )
                            }
                        }
                    }
                }

                is ToolbarLayoutState.NormalTabs -> {
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

                        LazyRow(
                            modifier = Modifier.weight(1f),
                        ) {
                            items(currentLayoutState.tabs.size) { index ->
                                val tab = currentLayoutState.tabs[index]
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

                        if (currentLayoutState.showAddCouponButton) {
                            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                            WooPosCircularIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = stringResource(
                                    id = R.string.woopos_coupons_empty_list_create_coupon_label,
                                ),
                                onClick = { onAddCouponEvent() }
                            )
                        }
                    }

                    when (val search = currentLayoutState.search) {
                        SearchState.Hidden -> Unit
                        is SearchState.Visible -> {
                            Box {
                                WooPosSearchInput(
                                    state = search.state,
                                    animationDuration = ANIMATION_DURATION,
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
}

private sealed class ToolbarLayoutState {
    data class NormalTabs(
        val tabs: List<WooPosItemsToolbarViewState.Tab>,
        val search: SearchState,
        val showAddCouponButton: Boolean,
    ) : ToolbarLayoutState()

    data class TabsWithBackButton(
        val tabs: List<WooPosItemsToolbarViewState.Tab>,
        val search: SearchState,
        val showAddCouponButton: Boolean,
    ) : ToolbarLayoutState()

    data class SearchOpen(
        val search: SearchState.Visible
    ) : ToolbarLayoutState()
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
        WooPosItemsToolbarViewState.Tab.ProductTab(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.CouponTab(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.ProductList(
                tabs = tabs,
                search = SearchState.Hidden,
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
        WooPosItemsToolbarViewState.Tab.ProductTab(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.CouponTab(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal
        ),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsToolbarViewState.CouponList(tabs = tabs),
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
        WooPosItemsToolbarViewState.Tab.ProductTab(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full
        ),
        WooPosItemsToolbarViewState.Tab.CouponTab(
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
                        hasAnimationPlayed = false,
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
        WooPosItemsToolbarViewState.Tab.VariationTab(
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
