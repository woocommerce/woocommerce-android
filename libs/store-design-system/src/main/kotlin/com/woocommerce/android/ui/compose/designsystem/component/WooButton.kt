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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
    maxLines: Int = DEFAULT_BUTTON_LABEL_MAX_LINES,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        maxLines = maxLines,
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
    maxLines: Int = DEFAULT_BUTTON_LABEL_MAX_LINES,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        maxLines = maxLines,
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
    maxLines: Int = DEFAULT_BUTTON_LABEL_MAX_LINES,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    WooButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
        maxLines = maxLines,
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
    maxLines: Int,
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
            maxLines = maxLines,
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
    maxLines: Int,
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
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun WooButtonStyle.toButtonColors(): ButtonColors {
    val colors = WooTheme.colors

    return when (this) {
        WooButtonStyle.Filled -> ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.stateLayers.onSurface.opacity10,
            disabledContentColor = colors.stateLayers.onSurface.opacity24,
        )
        WooButtonStyle.FilledTonal -> ButtonDefaults.filledTonalButtonColors(
            containerColor = colors.container.secondaryContainer,
            contentColor = colors.container.onSecondaryContainer,
            disabledContainerColor = colors.stateLayers.onSurface.opacity10,
            disabledContentColor = colors.stateLayers.onSurface.opacity24,
        )
        WooButtonStyle.Outlined -> ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = colors.container.onSecondaryContainer,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.stateLayers.onSurface.opacity24,
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
                    colors.stateLayers.onSurface.opacity16
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
@Preview(name = "Label behavior at 200%", widthDp = 360, fontScale = 2f, showBackground = true)
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
        WooOutlinedButton(
            text = "Compact small button label",
            onClick = {},
            modifier = Modifier.width(COMPACT_LABEL_PREVIEW_WIDTH),
            size = WooButtonSize.Small,
        )
        WooFilledButton(
            text = "Default medium label limited to two lines",
            onClick = {},
            modifier = Modifier.width(LONG_LABEL_PREVIEW_WIDTH),
            leadingIcon = { LeadingButtonIcon() },
        )
        WooFilledTonalButton(
            text = "Explicit one-line label",
            onClick = {},
            modifier = Modifier.width(LONG_LABEL_PREVIEW_WIDTH),
            maxLines = 1,
            leadingIcon = { LeadingButtonIcon() },
        )
        WooOutlinedButton(
            text = "Explicit unlimited label wraps without an ellipsis",
            onClick = {},
            modifier = Modifier.width(LONG_LABEL_PREVIEW_WIDTH),
            maxLines = Int.MAX_VALUE,
            leadingIcon = { LeadingButtonIcon() },
        )
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
private val COMPACT_LABEL_PREVIEW_WIDTH = 132.dp
private val LONG_LABEL_PREVIEW_WIDTH = 200.dp
private const val DEFAULT_BUTTON_LABEL_MAX_LINES = 2
