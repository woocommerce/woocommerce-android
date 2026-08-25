package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Bolt
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.CirclePlus
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark

@Composable
fun WooNoticeBanner(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: WooNoticeBannerTone = WooNoticeBannerTone.Neutral,
    leadingIcon: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    dismissContentDescription: String? = null,
    onDismissClick: (() -> Unit)? = null,
) {
    require((actionLabel == null) == (onActionClick == null)) {
        "WooNoticeBanner action label and callback must be provided together"
    }
    require((dismissContentDescription == null) == (onDismissClick == null)) {
        "WooNoticeBanner dismiss label and callback must be provided together"
    }
    require(actionLabel == null || actionLabel.isNotBlank()) { "WooNoticeBanner action label must not be blank" }
    require(dismissContentDescription == null || dismissContentDescription.isNotBlank()) {
        "WooNoticeBanner dismiss content description must not be blank"
    }
    val colors = tone.toNoticeBannerColors()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        shape = RoundedCornerShape(WooTheme.radius.medium),
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
                        modifier = Modifier.size(WooTheme.iconSize.size24),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingIcon()
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
            ) {
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
                if (actionLabel != null && onActionClick != null) {
                    TextButton(
                        onClick = onActionClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.contentColor),
                    ) {
                        Text(
                            text = actionLabel,
                            style = WooTheme.text.labelLarge.emphasized,
                        )
                    }
                }
            }
            if (dismissContentDescription != null && onDismissClick != null) {
                IconButton(
                    onClick = onDismissClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = colors.contentColor),
                ) {
                    Icon(
                        imageVector = WooIcons.Regular.Xmark,
                        contentDescription = dismissContentDescription,
                        modifier = Modifier.size(WooTheme.iconSize.size24),
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
            WooNoticeBannerDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooNoticeBannerDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        WooNoticeBanner(
            title = "Orders synced",
            description = "New order data is available.",
            tone = WooNoticeBannerTone.Success,
            leadingIcon = {
                Icon(
                    imageVector = WooIcons.Solid.CirclePlus,
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            },
        )
        WooNoticeBanner(
            title = "Manual review needed",
            description = "Some settings need another look.",
            tone = WooNoticeBannerTone.NeutralOutlined,
            leadingIcon = {
                Icon(
                    imageVector = WooIcons.Regular.CircleInfo,
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            },
        )
        WooNoticeBanner(
            title = "Connection issue",
            description = "Some analytics data may be delayed.",
            tone = WooNoticeBannerTone.Warning,
            leadingIcon = {
                Icon(
                    imageVector = WooIcons.Regular.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            },
        )
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
            contentColor = colors.surface.onDefault,
            border = BorderStroke(WooTheme.stroke.extraThin, colors.outlineVariant),
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
