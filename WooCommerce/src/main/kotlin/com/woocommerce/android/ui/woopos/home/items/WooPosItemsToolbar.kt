package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState.Open.Input
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab.HighlightLevel.Full
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab.HighlightLevel.Normal

private const val ANIMATION_DURATION = 300

@Composable
fun WooPosItemsToolbar(
    modifier: Modifier = Modifier,
    state: WooPosItemsViewState,
    onTabClicked: (WooPosItemsViewState.Tab) -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
) {
    val isSearchOpen = (state.search as? SearchState.Visible)?.let {
        it.state is WooPosSearchInputState.Open
    } == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedVisibility(
            visible = !isSearchOpen,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION,
                    delayMillis = ANIMATION_DURATION / 2,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = ANIMATION_DURATION / 2,
                    easing = FastOutSlowInEasing
                )
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = WooPosSpacing.Medium.value.toAdaptivePadding()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.tabs.forEach { tab ->
                    WooPosText(
                        text = stringResource(id = tab.stringId),
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

        when (val search = state.search) {
            SearchState.Hidden -> Unit
            is SearchState.Visible -> {
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
            state = WooPosItemsViewState.ProductList(
                tabs = tabs,
                search = SearchState.Hidden,
            ),
            onTabClicked = {},
            onSearchEvent = {}
        )
    }
}

@Composable
@WooPosPreview
fun WooPosItemsToolbarWithSearchPreview() {
    val tabs = listOf(
        WooPosItemsViewState.Tab(R.string.woopos_products_screen_title, highlightLevel = Full),
        WooPosItemsViewState.Tab(R.string.woopos_coupons_screen_title, highlightLevel = Normal),
    )

    WooPosTheme {
        WooPosItemsToolbar(
            state = WooPosItemsViewState.ProductList(
                tabs = tabs,
                search = SearchState.Visible(
                    state = WooPosSearchInputState.Open(
                        input = Input.Query(
                            query = "",
                            cursorPosition = 1,
                        ),
                        isLoading = false,
                        hasAnimationPlayed = false,
                    )
                )
            ),
            onTabClicked = {},
            onSearchEvent = {}
        )
    }
}
