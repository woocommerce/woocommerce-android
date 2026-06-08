package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.heightIn(min = MIN_TOUCH_TARGET_SIZE),
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = WooTheme.colors.onPrimary,
            checkedTrackColor = WooTheme.colors.primary,
            checkedBorderColor = WooTheme.colors.primary,
        ),
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooSwitchPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Row(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
            ) {
                WooSwitch(checked = true, onCheckedChange = {})
                WooSwitch(checked = false, onCheckedChange = {})
                WooSwitch(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
    }
}

private val MIN_TOUCH_TARGET_SIZE = 48.dp
