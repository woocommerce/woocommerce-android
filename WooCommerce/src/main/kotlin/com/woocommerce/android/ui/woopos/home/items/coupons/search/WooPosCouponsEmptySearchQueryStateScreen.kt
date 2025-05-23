package com.woocommerce.android.ui.woopos.home.items.coupons.search

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
import androidx.compose.foundation.layout.width
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

@Composable
private fun RecentSearchesChips(
    recentSearches: List<String>,
    onRecentSearchClicked: (String) -> Unit,
) {
    Column {
        SectionHeader(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding()),
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
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value.toAdaptivePadding()))

            recentSearches.forEach { recentSearch ->
                WooPosChip(
                    text = recentSearch,
                    onClick = { onRecentSearchClicked(recentSearch) },
                    leadingIcon = Icons.Default.Search
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value.toAdaptivePadding()))
        }
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String
) {
    WooPosText(
        modifier = modifier,
        text = title,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
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
