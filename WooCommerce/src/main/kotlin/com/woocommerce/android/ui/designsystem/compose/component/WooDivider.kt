package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.DefaultWooStroke
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

@Composable
fun WooDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = DefaultWooStroke.extraThin,
        color = WooTheme.colors.outlineVariant,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooDividerPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            ) {
                WooDivider()
                Row(modifier = Modifier.height(48.dp)) {
                    WooVerticalDivider()
                }
            }
        }
    }
}

@Composable
fun WooVerticalDivider(
    modifier: Modifier = Modifier,
) {
    VerticalDivider(
        modifier = modifier,
        thickness = DefaultWooStroke.extraThin,
        color = WooTheme.colors.outlineVariant,
    )
}
