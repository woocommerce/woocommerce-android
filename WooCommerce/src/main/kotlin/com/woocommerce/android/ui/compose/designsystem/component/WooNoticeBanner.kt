package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.DefaultWooStroke
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooNoticeBanner(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: WooNoticeBannerTone = WooNoticeBannerTone.Neutral,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = tone.toNoticeBannerColors()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = MaterialTheme.shapes.medium,
        border = colors.border,
    ) {
        Row(
            modifier = Modifier.padding(WooTheme.padding.padding4),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.Top,
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    Box(
                        modifier = Modifier.size(NOTICE_ICON_SIZE),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingIcon()
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1)) {
                Text(
                    text = title,
                    style = WooTheme.text.titleMedium.emphasized,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = WooTheme.text.bodyMedium.regular,
                    )
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooNoticeBannerPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooNoticeBanner(
                    title = "Orders synced",
                    description = "New order data is available.",
                    tone = WooNoticeBannerTone.Success,
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(NOTICE_ICON_SIZE),
                        )
                    },
                )
                WooNoticeBanner(
                    title = "Manual review needed",
                    description = "Some settings need another look.",
                    tone = WooNoticeBannerTone.NeutralOutlined,
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                            contentDescription = null,
                            modifier = Modifier.size(NOTICE_ICON_SIZE),
                        )
                    },
                )
                WooNoticeBanner(
                    title = "Connection issue",
                    description = "Some analytics data may be delayed.",
                    tone = WooNoticeBannerTone.Warning,
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_warning_filled_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(NOTICE_ICON_SIZE),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun WooNoticeBannerTone.toNoticeBannerColors(): WooNoticeBannerColors {
    val colors = WooTheme.colors
    return when (this) {
        WooNoticeBannerTone.Neutral -> WooNoticeBannerColors(
            colors.status.neutralContainer,
            colors.status.onNeutralContainer,
        )
        WooNoticeBannerTone.NeutralOutlined -> WooNoticeBannerColors(
            containerColor = colors.surface.default,
            contentColor = colors.status.onNeutralOutlinedContainer,
            border = BorderStroke(DefaultWooStroke.extraThin, colors.status.neutralOutlinedContainer),
        )
        WooNoticeBannerTone.Info -> WooNoticeBannerColors(colors.status.infoContainer, colors.status.onInfoContainer)
        WooNoticeBannerTone.Success -> {
            WooNoticeBannerColors(colors.status.successContainer, colors.status.onSuccessContainer)
        }
        WooNoticeBannerTone.Warning -> {
            WooNoticeBannerColors(colors.status.warningContainer, colors.status.onWarningContainer)
        }
        WooNoticeBannerTone.Caution -> {
            WooNoticeBannerColors(colors.status.cautionContainer, colors.status.onCautionContainer)
        }
        WooNoticeBannerTone.Error -> WooNoticeBannerColors(colors.status.errorContainer, colors.status.onErrorContainer)
    }
}

private data class WooNoticeBannerColors(
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke? = null,
)

private val NOTICE_ICON_SIZE = 24.dp
