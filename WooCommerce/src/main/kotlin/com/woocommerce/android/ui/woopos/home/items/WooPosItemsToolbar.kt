package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab.HighlightLevel.Full
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab.HighlightLevel.Normal

@Composable
fun WooPosItemsToolbar(
    state: WooPosItemsViewState,
    onTabClicked: (WooPosItemsViewState.Tab) -> Unit,
    onToolbarInfoIconClicked: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
) {
    val isSearchExpanded = state is WooPosItemsViewState.Content &&
        state.search is WooPosItemsViewState.Content.SearchState.Visible &&
        state.search.state is WooPosSearchInputState.Open

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedVisibility(
            visible = !isSearchExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Row {
                state.tabs.forEach { tab ->
                    WooPosText(
                        text = stringResource(id = tab.stringId),
                        style = WooPosTypography.Heading,
                        fontWeight = FontWeight.Bold,
                        color = tab.highlightLevel.titleColor(),
                        modifier = Modifier.clickable(
                            onClick = { onTabClicked(tab) },
                        )
                    )
                    Spacer(modifier = Modifier.width(WooPosSpacing.Large.value))
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state) {
                is WooPosItemsViewState.Content -> {
                    when (val searchState = state.search) {
                        WooPosItemsViewState.Content.SearchState.Hidden -> Unit
                        is WooPosItemsViewState.Content.SearchState.Visible -> {
                            WooPosSearchInput(
                                state = searchState.state,
                                onEvent = onSearchEvent,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))

                            VerticalDivider(
                                modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
                                thickness = 1.dp,
                            )

                            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
                        }
                    }

                    if (state.bannerState.isBannerHiddenByUser) {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                onToolbarInfoIconClicked()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(
                                    id = R.string.woopos_banner_simple_products_info_content_description
                                ),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                is WooPosItemsViewState.Empty,
                is WooPosItemsViewState.Error,
                is WooPosItemsViewState.Loading -> Unit
            }
        }
    }
}

@Composable
private fun WooPosItemsViewState.Tab.HighlightLevel.titleColor(): Color = when (this) {
    Full -> MaterialTheme.colorScheme.onSurface
    Normal -> WooPosTheme.colors.onSurfaceVariantLowest
}

@Composable
@WooPosPreview
fun WooPosItemsToolbarPreview() {
    val tabs = listOf(
        WooPosItemsViewState.Tab(R.string.woopos_products_screen_title, highlightLevel = Full),
        WooPosItemsViewState.Tab(R.string.woopos_coupons_screen_title, highlightLevel = Normal),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsViewState.Content(
                items = emptyList(),
                paginationState = WooPosPaginationState.Error,
                pullToRefreshState = WooPosPullToRefreshState.Refreshing,
                bannerState = WooPosItemsViewState.Content.BannerState(
                    isBannerHiddenByUser = true,
                    title = R.string.woopos_banner_simple_products_only_title,
                    message = R.string.woopos_banner_simple_products_only_message,
                    icon = R.drawable.info,
                ),
                tabs = tabs,
                search = WooPosItemsViewState.Content.SearchState.Hidden
            ),
            onTabClicked = {},
            onToolbarInfoIconClicked = {},
            onSearchEvent = {}
        )
    }
}
