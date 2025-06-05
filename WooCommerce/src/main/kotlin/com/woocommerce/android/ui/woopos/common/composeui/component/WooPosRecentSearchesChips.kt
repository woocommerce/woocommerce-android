package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding

@Composable
fun WooPosRecentSearchesChips(
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
fun SectionHeader(
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
