package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowUpRight
import com.woocommerce.android.ui.compose.designsystem.icons.CommentQuestion
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
fun WooIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: WooIconButtonEmphasis = WooIconButtonEmphasis.Neutral,
) {
    WooIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        emphasis = emphasis,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(WooTheme.iconSize.size24),
        )
    }
}

@Composable
fun WooIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: WooIconButtonEmphasis = WooIconButtonEmphasis.Neutral,
    icon: @Composable () -> Unit,
) {
    assert(contentDescription.isNotBlank()) {
        "WooIconButton contentDescription must not be blank"
    }

    val colors = WooTheme.colors
    val contentColor = when (emphasis) {
        WooIconButtonEmphasis.Neutral -> colors.surface.onDefault
        WooIconButtonEmphasis.Primary -> colors.primary
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(MIN_TOUCH_TARGET_SIZE)
            .semantics {
                this.contentDescription = contentDescription
            },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor,
            disabledContentColor = colors.surface.onVariantLowest,
        ),
    ) {
        Box(
            modifier = Modifier.clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooIconButtonPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooIconButtonDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooIconButtonDemo(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        WooIconButton(
            imageVector = WooIcons.Regular.CommentQuestion,
            contentDescription = "",
            onClick = {},
        )
        WooIconButton(
            imageVector = WooIcons.Regular.ArrowUpRight,
            contentDescription = "Open",
            onClick = {},
            emphasis = WooIconButtonEmphasis.Primary,
        )
        WooIconButton(
            imageVector = WooIcons.Regular.CommentQuestion,
            contentDescription = "Disabled help",
            onClick = {},
            enabled = false,
        )
    }
}

private val MIN_TOUCH_TARGET_SIZE = 48.dp
