package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.woocommerce.android.R

@Composable
private fun defaultSwitchColors(): SwitchColors =
    SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)

@Composable
fun BottomSheetSwitchColors(): SwitchColors =
    SwitchDefaults.colors(
        checkedThumbColor = colorResource(id = R.color.color_primary),
        checkedTrackColor = colorResource(id = R.color.color_primary),
        uncheckedThumbColor =
        when {
            isSystemInDarkTheme() -> colorResource(id = R.color.color_on_surface_medium)
            else -> MaterialTheme.colors.onSurface
        }
    )

@Composable
fun WCSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = defaultSwitchColors()
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors
    )
}

@Composable
fun WCSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = defaultSwitchColors()
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1
        )
        WCSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = colors
        )
    }
}
