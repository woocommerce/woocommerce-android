package com.woocommerce.android.ui.woopos.home.items.coupons.search

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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.RecentSearchesChips
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding

@Composable
fun WooPosCouponsEmptySearchQueryStateScreen(
    modifier: Modifier = Modifier,
    state: WooPosCouponsSearchViewState.EmptySearchQuery,
    onUIEvent: (WooPosCouponsSearchUiEvent) -> Unit
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
                    onUIEvent(WooPosCouponsSearchUiEvent.OnRecentSearchClicked(recentSearch))
                }
            )
        }

        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(104.dp))
    }
}

@WooPosPreview
@Composable
fun WooPosCouponsEmptySearchQueryStatePreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosCouponsEmptySearchQueryStateScreen(
                state = WooPosCouponsSearchViewState.EmptySearchQuery(
                    recentSearches = listOf("SUMMER", "DISCOUNT", "SALE", "WINTER"),
                ),
                onUIEvent = { },
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCouponsRecentSearchesChipsPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value)
        ) {
            RecentSearchesChips(
                recentSearches = listOf("SUMMER", "DISCOUNT", "SALE", "WINTER", "PROMO"),
                onRecentSearchClicked = {}
            )
        }
    }
}
