package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
internal fun WooDesignSystemPreviewTheme(
    content: @Composable () -> Unit,
) {
    WooDesignSystemTheme {
        Surface(
            color = WooTheme.colors.background.section,
            contentColor = WooTheme.colors.surface.onDefault,
        ) {
            content()
        }
    }
}
