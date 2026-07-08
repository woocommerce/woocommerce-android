package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    WooSectionHeader(
        text = text,
        modifier = modifier,
        onTextLayout = {},
    )
}

@Composable
internal fun WooSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = WooTheme.padding.padding7,
                vertical = WooTheme.padding.padding6,
            ),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            color = WooTheme.colors.surface.onVariantLowest,
            onTextLayout = onTextLayout,
            style = WooTheme.text.titleSmall.emphasized,
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSectionHeaderPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooSectionHeader(
                text = "Tracking",
            )
        }
    }
}
