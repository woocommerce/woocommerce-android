package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooFilledButton(
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
        style = WooButtonStyle.Filled,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun WooFilledTonalButton(
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
        style = WooButtonStyle.FilledTonal,
        leadingIcon = leadingIcon,
    )
}

@Composable
fun WooOutlinedButton(
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
        style = WooButtonStyle.Outlined,
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
    val buttonColors = style.toButtonColors()
    val shape = RoundedCornerShape(buttonSpec.radius)
    val buttonModifier = modifier.heightIn(min = buttonSpec.visualHeight)
    val contentPadding = PaddingValues(
        horizontal = WooTheme.padding.padding5,
    )
    val content: @Composable () -> Unit = {
        WooButtonContent(
            text = text,
            buttonSpec = buttonSpec,
            leadingIcon = leadingIcon,
        )
    }

    when (style) {
        WooButtonStyle.Filled -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = buttonColors,
            elevation = null,
            contentPadding = contentPadding,
            content = { content() },
        )
        WooButtonStyle.FilledTonal -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = buttonColors,
            elevation = null,
            contentPadding = contentPadding,
            content = { content() },
        )
        WooButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = buttonColors,
            border = style.toButtonBorder(enabled),
            contentPadding = contentPadding,
            content = { content() },
        )
    }
}

@Composable
private fun WooButtonContent(
    text: String,
    buttonSpec: WooButtonSpec,
    leadingIcon: @Composable (() -> Unit)?,
) {
    if (leadingIcon != null) {
        Box(
            modifier = Modifier.size(buttonSpec.iconSize),
            contentAlignment = Alignment.Center,
        ) {
            leadingIcon()
        }
        Spacer(modifier = Modifier.width(WooTheme.spacing.space3))
    }
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = buttonSpec.textStyle,
    )
}

@Composable
private fun WooButtonStyle.toButtonColors(): ButtonColors {
    val colors = WooTheme.colors
    val disabledStateLayerColor = colors.surface.onDefault.copy(alpha = DISABLED_STATE_LAYER_ALPHA)
    val disabledContentColor = colors.surface.onDefault.copy(alpha = DISABLED_CONTENT_ALPHA)

    return when (this) {
        WooButtonStyle.Filled -> ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = disabledStateLayerColor,
            disabledContentColor = disabledContentColor,
        )
        WooButtonStyle.FilledTonal -> ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.container.secondaryContainer,
            contentColor = colors.container.onSecondaryContainer,
            disabledContainerColor = disabledStateLayerColor,
            disabledContentColor = disabledContentColor,
        )
        WooButtonStyle.Outlined -> ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.container.onSecondaryContainer,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor,
        )
    }
}

@Composable
private fun WooButtonStyle.toButtonBorder(enabled: Boolean): BorderStroke? =
    when (this) {
        WooButtonStyle.Outlined -> {
            val colors = WooTheme.colors
            BorderStroke(
                width = WooTheme.stroke.medium,
                color = if (enabled) {
                    colors.container.secondaryContainer
                } else {
                    colors.surface.onDefault.copy(alpha = DISABLED_OUTLINED_BORDER_ALPHA)
                },
            )
        }
        else -> null
    }

private enum class WooButtonStyle {
    Filled,
    FilledTonal,
    Outlined,
}

@Composable
private fun LeadingButtonIcon() {
    Icon(
        imageVector = WooIcons.Regular.Star,
        contentDescription = null,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooButtonPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooButtonDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooButtonDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        WooFilledButton(
            text = "Label",
            onClick = {},
            leadingIcon = { LeadingButtonIcon() },
        )
        WooFilledTonalButton(
            text = "Filled tonal",
            onClick = {},
            leadingIcon = { LeadingButtonIcon() },
        )
        WooOutlinedButton(
            text = "Outlined",
            onClick = {},
            leadingIcon = { LeadingButtonIcon() },
        )
        WooFilledButton(
            text = "Small",
            onClick = {},
            size = WooButtonSize.Small,
            leadingIcon = { LeadingButtonIcon() },
        )
        WooFilledButton(text = "Disabled", onClick = {}, enabled = false)
    }
}

@Composable
private fun WooButtonSize.toButtonSpec(): WooButtonSpec =
    when (this) {
        WooButtonSize.Medium -> WooButtonSpec(
            visualHeight = MEDIUM_BUTTON_VISUAL_HEIGHT,
            radius = WooTheme.radius.extraLarge,
            iconSize = WooTheme.iconSize.size18,
            textStyle = WooTheme.text.labelLarge.emphasized,
        )
        WooButtonSize.Small -> WooButtonSpec(
            visualHeight = SMALL_BUTTON_VISUAL_HEIGHT,
            radius = WooTheme.radius.large,
            iconSize = WooTheme.iconSize.size14,
            textStyle = WooTheme.text.labelMedium.emphasized,
        )
    }

private data class WooButtonSpec(
    val visualHeight: Dp,
    val radius: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

private val MEDIUM_BUTTON_VISUAL_HEIGHT = 56.dp
private val SMALL_BUTTON_VISUAL_HEIGHT = 32.dp
private const val DISABLED_STATE_LAYER_ALPHA = 0.08f
private const val DISABLED_OUTLINED_BORDER_ALPHA = 0.10f
private const val DISABLED_CONTENT_ALPHA = 0.24f
