package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

@Composable
fun WooSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = WooTheme.colors.primary,
        style = WooTheme.text.labelLarge.emphasized,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSectionHeaderPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooSectionHeader(
                text = "Tracking",
                modifier = Modifier.padding(WooTheme.padding.padding5),
            )
        }
    }
}
