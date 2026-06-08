package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooCellContent(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val colors = WooTheme.colors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
    ) {
        Text(
            text = title,
            color = if (enabled) colors.surface.onDefault else colors.surface.onLowest,
            style = WooTheme.text.titleMedium.emphasized,
        )
        if (description != null) {
            Text(
                text = description,
                color = if (enabled) colors.surface.onVariant else colors.surface.onLowest,
                style = WooTheme.text.bodyMedium.regular,
            )
        }
    }
}

/**
 * Use either a row-owned [onClick] action or independently clickable slot actions, not both for the same action.
 */
@Composable
fun WooCell(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        modifier
    }

    WooCellLayout(
        title = title,
        description = description,
        enabled = enabled,
        density = WooCellLayoutDensity.Generic,
        modifier = rowModifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
    )
}

@Composable
internal fun WooCellLayout(
    title: String,
    description: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    density: WooCellLayoutDensity = WooCellLayoutDensity.Compact,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val metrics = density.toMetrics()
    val slotContentColor = if (enabled) {
        WooTheme.colors.surface.onDefault
    } else {
        WooTheme.colors.surface.onLowest
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = metrics.minHeight)
            .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = metrics.contentGap),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides slotContentColor) {
                    leadingContent()
                }
            }
        }

        WooCellContent(
            title = title,
            description = description,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )

        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .padding(start = metrics.contentGap)
                    .widthIn(min = metrics.trailingMinWidth),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides slotContentColor) {
                    trailingContent()
                }
            }
        }
    }
}

internal enum class WooCellLayoutDensity {
    Compact,
    Generic,
}

@Composable
private fun WooCellLayoutDensity.toMetrics(): WooCellLayoutMetrics =
    when (this) {
        WooCellLayoutDensity.Compact -> WooCellLayoutMetrics(
            minHeight = MIN_TOUCH_TARGET_SIZE,
            horizontalPadding = WooTheme.padding.padding5,
            verticalPadding = WooTheme.padding.padding3,
            contentGap = WooTheme.spacing.space4,
            trailingMinWidth = MIN_TOUCH_TARGET_SIZE,
        )
        WooCellLayoutDensity.Generic -> WooCellLayoutMetrics(
            minHeight = GENERIC_CELL_MIN_HEIGHT,
            horizontalPadding = WooTheme.padding.padding7,
            verticalPadding = WooTheme.padding.padding7,
            contentGap = WooTheme.spacing.space6,
            trailingMinWidth = MIN_TOUCH_TARGET_SIZE,
        )
    }

private data class WooCellLayoutMetrics(
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentGap: Dp,
    val trailingMinWidth: Dp,
)

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooCellPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(modifier = Modifier.padding(vertical = WooTheme.padding.padding3)) {
                WooCell(
                    title = "Store details",
                    description = "Manage address, currency, and contact information.",
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_info_filled_24dp),
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                            contentDescription = null,
                        )
                    },
                )
                WooDivider()
                WooCell(
                    title = "Disabled cell",
                    description = "This row cannot be opened.",
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

private val MIN_TOUCH_TARGET_SIZE = 48.dp
private val GENERIC_CELL_MIN_HEIGHT = 90.dp
