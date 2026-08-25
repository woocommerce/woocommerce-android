package com.woocommerce.android.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
internal fun HttpsConfigurationWarningBanner(
    title: String,
    description: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    dismissContentDescription: String,
    onDismissClick: () -> Unit,
) {
    WooDesignSystemTheme {
        WooNoticeBanner(
            title = title,
            description = description,
            tone = WooNoticeBannerTone.Warning,
            actionLabel = actionLabel,
            onActionClick = onActionClick,
            dismissContentDescription = dismissContentDescription,
            onDismissClick = onDismissClick,
            modifier = Modifier.padding(
                horizontal = WooTheme.padding.padding4,
                vertical = WooTheme.padding.padding3,
            ),
        )
    }
}
