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
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosRecentSearchesChips
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

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
            .padding(top = WooPosSpacing.Small.value)
            .padding(top = WooPosSpacing.XSmall.value)
            .verticalScroll(scrollState)
    ) {
        if (state.recentSearches.isNotEmpty()) {
            WooPosRecentSearchesChips(
                recentSearches = state.recentSearches,
                onRecentSearchClicked = { recentSearch ->
                    onUIEvent(WooPosCouponsSearchUiEvent.OnRecentSearchClicked(recentSearch))
                }
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Gigantic.value))
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
            WooPosRecentSearchesChips(
                recentSearches = listOf("SUMMER", "DISCOUNT", "SALE", "WINTER", "PROMO"),
                onRecentSearchClicked = {}
            )
        }
    }
}
