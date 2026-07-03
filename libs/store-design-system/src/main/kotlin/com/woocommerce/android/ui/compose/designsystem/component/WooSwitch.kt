package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            WooSwitchDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooSwitchDemo(
    modifier: Modifier = Modifier,
) {
    var checkedSwitchChecked by rememberSaveable { mutableStateOf(true) }
    var uncheckedSwitchChecked by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
    ) {
        WooSwitch(checked = checkedSwitchChecked, onCheckedChange = { checkedSwitchChecked = it })
        WooSwitch(checked = uncheckedSwitchChecked, onCheckedChange = { uncheckedSwitchChecked = it })
        WooSwitch(checked = true, onCheckedChange = {}, enabled = false)
    }
}

private val MIN_TOUCH_TARGET_SIZE = 48.dp
