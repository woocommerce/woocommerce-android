package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

/**
 * Anchored dropdown menu that owns its expanded state.
 *
 * The design system owns the anchor, the container styling, and dismissal. [trigger] receives the callback that
 * opens the menu, so callers decide what the anchor looks like. [content] receives the callback that closes the
 * menu; invoke it before running a selected action so the menu never stays open behind a navigation.
 *
 * For top app bar actions prefer [WooTopAppBarActionsScope.OverflowAction], which supplies the standard outlined
 * ellipsis trigger.
 */
@Composable
fun WooOverflowMenu(
    trigger: @Composable (onClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        trigger { isExpanded = true }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = WooTheme.colors.surface.default,
        ) {
            content { isExpanded = false }
        }
    }
}

/**
 * Text item for [WooOverflowMenu]. [isDestructive] switches the label to the error color.
 */
@Composable
fun WooOverflowMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
    require(text.isNotBlank()) {
        "WooOverflowMenuItem text must not be blank"
    }
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = if (isDestructive) {
                    WooTheme.colors.error
                } else {
                    WooTheme.colors.surface.onDefault
                },
                style = WooTheme.text.bodyLarge.regular,
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooOverflowMenuPreview() {
    WooDesignSystemTheme {
        WooOverflowMenu(
            trigger = { onClick ->
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.Ellipsis,
                    contentDescription = "More options",
                    onClick = onClick,
                )
            },
        ) { dismiss ->
            WooOverflowMenuItem(text = "Duplicate", onClick = dismiss)
            WooOverflowMenuItem(text = "Delete", onClick = dismiss, isDestructive = true)
        }
    }
}
