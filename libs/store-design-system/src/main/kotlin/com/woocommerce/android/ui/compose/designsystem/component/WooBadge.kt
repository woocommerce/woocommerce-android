package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.WooDsIcons

@Composable
fun WooBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: WooBadgeTone = WooBadgeTone.Neutral,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = tone.toBadgeColors()

    Surface(
        modifier = modifier.heightIn(min = BADGE_MIN_HEIGHT),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        border = colors.border,
        shape = RoundedCornerShape(WooTheme.radius.medium),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = WooTheme.padding.padding3,
            ),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                    Box(
                        modifier = Modifier.size(WooTheme.iconSize.size14),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingIcon()
                    }
                }
            }
            Text(
                text = text,
                style = WooTheme.text.bodySmall.regular,
            )
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooBadgePreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooBadgeDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooBadgeDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
            WooBadge("Error", tone = WooBadgeTone.Error, leadingIcon = { BadgeLeadingIcon() })
            WooBadge("Caution", tone = WooBadgeTone.Caution, leadingIcon = { BadgeLeadingIcon() })
            WooBadge("Warning", tone = WooBadgeTone.Warning, leadingIcon = { BadgeLeadingIcon() })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
            WooBadge("Success", tone = WooBadgeTone.Success, leadingIcon = { BadgeLeadingIcon() })
            WooBadge("Info", tone = WooBadgeTone.Info, leadingIcon = { BadgeLeadingIcon() })
            WooBadge("Neutral", tone = WooBadgeTone.Neutral, leadingIcon = { BadgeLeadingIcon() })
            WooBadge(
                "Outlined",
                tone = WooBadgeTone.NeutralOutlined,
                leadingIcon = { BadgeLeadingIcon() },
            )
        }
    }
}

@Composable
private fun BadgeLeadingIcon() {
    Icon(
        imageVector = WooDsIcons.Regular.Star,
        contentDescription = null,
    )
}

@Composable
private fun WooBadgeTone.toBadgeColors(): WooBadgeColors {
    val colors = WooTheme.colors
    return when (this) {
        WooBadgeTone.Neutral -> WooBadgeColors(colors.status.neutralContainer, colors.status.onNeutralContainer)
        WooBadgeTone.NeutralOutlined -> WooBadgeColors(
            containerColor = Color.Transparent,
            contentColor = colors.surface.onDefault,
            border = BorderStroke(WooTheme.stroke.regular, colors.outlineVariant),
        )
        WooBadgeTone.Info -> WooBadgeColors(colors.status.infoContainer, colors.status.onInfoContainer)
        WooBadgeTone.Success -> WooBadgeColors(colors.status.successContainer, colors.status.onSuccessContainer)
        WooBadgeTone.Warning -> WooBadgeColors(colors.status.warningContainer, colors.status.onWarningContainer)
        WooBadgeTone.Caution -> WooBadgeColors(colors.status.cautionContainer, colors.status.onCautionContainer)
        WooBadgeTone.Error -> WooBadgeColors(colors.status.errorContainer, colors.status.onErrorContainer)
    }
}

private data class WooBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke? = null,
)

private val BADGE_MIN_HEIGHT = 24.dp
