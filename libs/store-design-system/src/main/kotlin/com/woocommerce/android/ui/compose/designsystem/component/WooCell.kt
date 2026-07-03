package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
internal fun WooCellContent(
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
            color = if (enabled) colors.surface.onDefault else colors.surface.onVariantLowest,
            style = WooTheme.text.bodyLarge.emphasized,
        )
        if (description != null) {
            Text(
                text = description,
                color = if (enabled) colors.surface.onVariant else colors.surface.onVariantLowest,
                style = WooTheme.text.bodyMedium.regular,
            )
        }
    }
}

@Composable
fun WooCellTrailingAffordance(
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
        contentDescription = null,
        modifier = modifier.size(WooTheme.iconSize.size18),
    )
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
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val slotContentColor = if (enabled) {
        WooTheme.colors.surface.onDefault
    } else {
        WooTheme.colors.surface.onVariantLowest
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET_SIZE)
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = WooTheme.spacing.space5),
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
                modifier = Modifier.padding(start = WooTheme.spacing.space5),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides slotContentColor) {
                    trailingContent()
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooCellPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooCellDemo(modifier = Modifier.padding(vertical = WooTheme.padding.padding3))
        }
    }
}

@Composable
internal fun WooCellDemo(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WooCell(
            title = "Cell title",
            description = "Cell with a title and description",
        )
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
            trailingContent = { WooCellTrailingAffordance() },
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

private val MIN_TOUCH_TARGET_SIZE = 48.dp
