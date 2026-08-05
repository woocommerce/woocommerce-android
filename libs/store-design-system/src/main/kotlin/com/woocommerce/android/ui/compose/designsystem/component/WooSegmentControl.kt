package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooSegmentControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    validateWooSegmentControl(options = options, selectedIndex = selectedIndex)

    val colors = WooTheme.colors
    val trackColor = if (enabled) {
        colors.tintLayers.primaryContainer.opacity16
    } else {
        colors.stateLayers.onSurface.opacity08
    }
    val trackShape = RoundedCornerShape(WooTheme.radius.full)

    Box(
        modifier = modifier
            .heightIn(min = MIN_INTERACTIVE_SIZE)
            .selectableGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(trackShape)
                .background(trackColor),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MIN_INTERACTIVE_SIZE)
                .padding(horizontal = TRACK_INSET),
        ) {
            options.forEachIndexed { index, label ->
                WooSegmentControlItem(
                    label = label,
                    selected = index == selectedIndex,
                    enabled = enabled,
                    colors = colors,
                    onClick = { onSelectedIndexChange(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

internal fun validateWooSegmentControl(options: List<String>, selectedIndex: Int) {
    require(options.size in MIN_OPTION_COUNT..MAX_OPTION_COUNT) {
        "WooSegmentControl requires between $MIN_OPTION_COUNT and $MAX_OPTION_COUNT options."
    }
    require(selectedIndex in options.indices) {
        "WooSegmentControl selectedIndex must identify an option."
    }
    require(options.all(String::isNotBlank)) {
        "WooSegmentControl options must have nonblank labels."
    }
}

@Composable
private fun WooSegmentControlItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    colors: WooColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val itemShape = RoundedCornerShape(WooTheme.radius.extraLarge)
    val containerColor = when {
        !enabled && selected -> colors.stateLayers.onSurface.opacity16
        selected -> colors.surface.bright
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> colors.stateLayers.onSurface.opacity24
        selected -> colors.surface.onDefault
        else -> colors.container.onPrimaryContainer
    }
    val textStyle = if (selected) WooTheme.text.bodySmall.emphasized else WooTheme.text.bodySmall.regular

    Box(
        modifier = modifier
            .height(MIN_INTERACTIVE_SIZE)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SELECTED_PILL_HEIGHT)
                .clip(itemShape)
                .background(containerColor)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                ),
        )
        Text(
            text = label,
            color = contentColor,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(horizontal = LABEL_HORIZONTAL_PADDING),
        )
    }
}

@PreviewLightDark
@Composable
private fun WooSegmentControlEnabledPreview() {
    WooDesignSystemTheme {
        WooSegmentControlDemo(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun WooSegmentControlDisabledPreview() {
    WooDesignSystemTheme {
        WooSegmentControl(
            options = listOf("Net sales", "Orders", "Visitors"),
            selectedIndex = 1,
            onSelectedIndexChange = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Preview(name = "Long labels, compact", widthDp = 320, showBackground = true)
@Composable
private fun WooSegmentControlLongLabelsPreview() {
    WooDesignSystemTheme {
        WooSegmentControl(
            options = listOf("Gross sales before refunds", "Net sales after refunds"),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(name = "Large font", fontScale = 2f, widthDp = 360, showBackground = true)
@Composable
private fun WooSegmentControlLargeFontPreview() {
    WooDesignSystemTheme {
        WooSegmentControl(
            options = listOf("Net sales", "Total sales", "Gross sales"),
            selectedIndex = 2,
            onSelectedIndexChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "RTL", locale = "ar", widthDp = 360, showBackground = true)
@Composable
private fun WooSegmentControlRtlPreview() {
    WooDesignSystemTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            WooSegmentControl(
                options = listOf("المبيعات", "الطلبات", "الزوار"),
                selectedIndex = 1,
                onSelectedIndexChange = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "Tablet", widthDp = 720, showBackground = true)
@Composable
private fun WooSegmentControlTabletPreview() {
    WooDesignSystemTheme {
        WooSegmentControl(
            options = listOf("Net sales", "Total sales", "Gross sales", "Orders", "Visitors"),
            selectedIndex = 3,
            onSelectedIndexChange = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
internal fun WooSegmentControlDemo(modifier: Modifier = Modifier) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    WooSegmentControl(
        options = listOf("Net sales", "Total sales", "Gross sales"),
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { selectedIndex = it },
        modifier = modifier,
    )
}

private const val MIN_OPTION_COUNT = 2
private const val MAX_OPTION_COUNT = 5
private val TRACK_HEIGHT = 36.dp
private val SELECTED_PILL_HEIGHT = 32.dp
private val TRACK_INSET = 2.dp
private val MIN_INTERACTIVE_SIZE = 48.dp
private val LABEL_HORIZONTAL_PADDING = 8.dp
