package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

/**
 * Use either a row-owned [onClick] action or an independently clickable slot action, not both for the same action.
 */
@Composable
fun WooSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    WooCell(
        title = title,
        description = description,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
    )
}

@Composable
fun WooSwitchSettingsRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    WooCellLayout(
        title = title,
        description = description,
        enabled = enabled,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailingContent = {
            WooSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSettingsRowPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Column(modifier = Modifier.padding(vertical = WooTheme.padding.padding3)) {
                WooSettingsRow(
                    title = "Store details",
                    description = "Manage address, currency, and contact information.",
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_settings_filled_24dp),
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
                WooSwitchSettingsRow(
                    title = "Usage analytics",
                    description = "Send anonymous usage data.",
                    checked = true,
                    onCheckedChange = {},
                )
                WooDivider()
                WooSettingsRow(
                    title = "Disabled row",
                    description = "This setting is unavailable.",
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}
