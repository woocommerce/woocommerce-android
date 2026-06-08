package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.DefaultWooStroke
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

enum class WooButtonSize {
    Medium,
    Small,
}

@Composable
fun WooPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: WooButtonSize = WooButtonSize.Medium,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        style = WooButtonStyle.Primary,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun WooSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: WooButtonSize = WooButtonSize.Medium,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        style = WooButtonStyle.Secondary,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun WooTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: WooButtonSize = WooButtonSize.Medium,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        style = WooButtonStyle.Tertiary,
        leadingIcon = leadingIcon,
    )
}

@Composable
private fun WooButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: WooButtonSize,
    style: WooButtonStyle,
    leadingIcon: @Composable (() -> Unit)?,
) {
    val buttonSpec = size.toButtonSpec()
    val buttonColors = style.toButtonColors(enabled)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .heightIn(min = buttonSpec.visualHeight)
                .widthIn(min = buttonSpec.minWidth),
            color = buttonColors.containerColor,
            contentColor = buttonColors.contentColor,
            border = buttonColors.border,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = WooTheme.padding.padding5),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier.size(buttonSpec.iconSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingIcon()
                    }
                }
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = buttonSpec.textStyle,
                )
            }
        }
    }
}

@Composable
private fun WooButtonStyle.toButtonColors(enabled: Boolean): WooButtonColors {
    val colors = WooTheme.colors
    val disabledContainerColor = colors.surface.onLowest.copy(alpha = DISABLED_CONTAINER_ALPHA)

    return when (this) {
        WooButtonStyle.Primary -> WooButtonColors(
            containerColor = if (enabled) colors.primary else disabledContainerColor,
            contentColor = if (enabled) colors.onPrimary else colors.surface.onLowest,
        )
        WooButtonStyle.Secondary -> WooButtonColors(
            containerColor = if (enabled) colors.secondary else disabledContainerColor,
            contentColor = if (enabled) colors.onSecondary else colors.surface.onLowest,
        )
        WooButtonStyle.Tertiary -> WooButtonColors(
            containerColor = Color.Transparent,
            contentColor = if (enabled) colors.onSecondary else colors.surface.onLowest,
            border = BorderStroke(
                width = DefaultWooStroke.medium,
                color = if (enabled) colors.secondary else colors.outlineVariant,
            ),
        )
    }
}

private data class WooButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val border: BorderStroke? = null,
)

private enum class WooButtonStyle {
    Primary,
    Secondary,
    Tertiary,
}

@Composable
private fun LeadingButtonIcon() {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_star_24dp),
        contentDescription = null,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooPrimaryButtonPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooPrimaryButton(
                    text = "Save changes",
                    onClick = {},
                    leadingIcon = { LeadingButtonIcon() },
                )
                WooSecondaryButton(
                    text = "Secondary",
                    onClick = {},
                    leadingIcon = { LeadingButtonIcon() },
                )
                WooTertiaryButton(
                    text = "Tertiary",
                    onClick = {},
                    leadingIcon = { LeadingButtonIcon() },
                )
                WooPrimaryButton(
                    text = "Small",
                    onClick = {},
                    size = WooButtonSize.Small,
                    leadingIcon = { LeadingButtonIcon() },
                )
                WooPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
            }
        }
    }
}

@Composable
private fun WooButtonSize.toButtonSpec(): WooButtonSpec =
    when (this) {
        WooButtonSize.Medium -> WooButtonSpec(
            visualHeight = MEDIUM_BUTTON_VISUAL_HEIGHT,
            minWidth = MEDIUM_BUTTON_MIN_WIDTH,
            iconSize = MEDIUM_BUTTON_ICON_SIZE,
            textStyle = WooTheme.text.labelLarge.emphasized,
        )
        WooButtonSize.Small -> WooButtonSpec(
            visualHeight = SMALL_BUTTON_VISUAL_HEIGHT,
            minWidth = SMALL_BUTTON_MIN_WIDTH,
            iconSize = SMALL_BUTTON_ICON_SIZE,
            textStyle = WooTheme.text.labelMedium.emphasized,
        )
    }

private data class WooButtonSpec(
    val visualHeight: Dp,
    val minWidth: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

private val MEDIUM_BUTTON_VISUAL_HEIGHT = 48.dp
private val SMALL_BUTTON_VISUAL_HEIGHT = 32.dp
private val MEDIUM_BUTTON_MIN_WIDTH = 58.dp
private val SMALL_BUTTON_MIN_WIDTH = 48.dp
private val MEDIUM_BUTTON_ICON_SIZE = 18.dp
private val SMALL_BUTTON_ICON_SIZE = 14.dp
private const val DISABLED_CONTAINER_ALPHA = 0.24f
