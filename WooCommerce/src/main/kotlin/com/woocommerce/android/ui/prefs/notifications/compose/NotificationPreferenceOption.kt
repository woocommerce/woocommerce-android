package com.woocommerce.android.ui.prefs.notifications.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
internal fun NotificationPreferenceOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = colorResource(id = R.color.color_on_surface_medium),
                disabledSelectedColor = colorResource(id = R.color.color_on_surface_medium).copy(alpha = 0.38f),
                disabledUnselectedColor = colorResource(id = R.color.color_on_surface_medium).copy(alpha = 0.38f)
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorResource(id = R.color.color_on_surface_high).let {
                    if (!enabled) it.copy(alpha = 0.38f) else it
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.color_on_surface_medium).let {
                    if (!enabled) it.copy(alpha = 0.38f) else it
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
