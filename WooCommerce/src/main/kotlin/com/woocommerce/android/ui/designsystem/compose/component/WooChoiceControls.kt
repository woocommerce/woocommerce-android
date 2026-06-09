package com.woocommerce.android.ui.designsystem.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.foundation.DefaultWooStroke
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemTheme

@Composable
fun WooCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = WooTheme.colors.primary,
            uncheckedColor = WooTheme.colors.outline,
            checkmarkColor = WooTheme.colors.onPrimary,
        ),
    )
}

@Composable
fun WooRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = RadioButtonDefaults.colors(
            selectedColor = WooTheme.colors.primary,
            unselectedColor = WooTheme.colors.outline,
        ),
    )
}

@Composable
fun WooFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = WooTheme.text.labelLarge.emphasized,
            )
        },
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            labelColor = WooTheme.colors.surface.onDefault,
            iconColor = WooTheme.colors.surface.onDefault,
            selectedContainerColor = WooTheme.colors.secondary,
            selectedLabelColor = WooTheme.colors.onSecondary,
            selectedLeadingIconColor = WooTheme.colors.onSecondary,
            disabledLabelColor = WooTheme.colors.surface.onLowest,
            disabledLeadingIconColor = WooTheme.colors.surface.onLowest,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = WooTheme.colors.outlineVariant,
            selectedBorderColor = WooTheme.colors.primary,
            disabledBorderColor = WooTheme.colors.outlineVariant,
            borderWidth = DefaultWooStroke.extraThin,
            selectedBorderWidth = DefaultWooStroke.extraThin,
        ),
    )
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooChoiceControlsPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            Row(
                modifier = Modifier.padding(WooTheme.padding.padding5),
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WooCheckbox(checked = true, onCheckedChange = {})
                WooCheckbox(checked = false, onCheckedChange = {}, enabled = false)
                WooRadioButton(selected = true, onClick = {})
                WooRadioButton(selected = false, onClick = {}, enabled = false)
                WooFilterChip(
                    selected = true,
                    onClick = {},
                    label = "Selected",
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(CHIP_ICON_SIZE),
                        )
                    },
                )
                WooFilterChip(selected = false, onClick = {}, label = "Filter")
            }
        }
    }
}

private val CHIP_ICON_SIZE = 18.dp
