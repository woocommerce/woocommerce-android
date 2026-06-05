@file:Suppress("UnusedPrivateMember")

package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.DefaultWooStroke
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@PreviewLightDark
@Composable
private fun WooDesignSystemFoundationPreview() {
    PreviewTheme {
        FoundationPreviewContent()
    }
}

@Preview(name = "Large Font", fontScale = 1.5f, showBackground = true)
@Composable
private fun WooDesignSystemFoundationLargeFontPreview() {
    PreviewTheme {
        FoundationPreviewContent()
    }
}

@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    WooDesignSystemTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

@Composable
private fun FoundationPreviewContent() {
    val spacing = WooTheme.spacing

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(spacing.space5),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        ColorFoundationSection()
        TypographyFoundationSection()
        SpacingAndPaddingFoundationSection()
        RadiusFoundationSection()
        StrokeFoundationSection()
        OmittedFoundationSection()
    }
}

@Composable
private fun ColorFoundationSection() {
    val colors = WooTheme.colors

    FoundationSection(title = "Color") {
        ColorSwatch("primary", colors.primary, colors.onPrimary)
        ColorSwatch("text.primary", colors.text.primary, colors.surface.default)
        ColorSwatch("icon.primary", colors.icon.primary, colors.surface.default)
        ColorSwatch("border.default", colors.border.default, colors.surface.default)
        ColorSwatch("status.success", colors.status.success, colors.surface.default)
        ColorSwatch("interactive.destructive", colors.interactive.destructive, colors.onPrimary)
        ColorSwatch("overlay.overlay20", colors.overlay.overlay20, colors.surface.default)
        ColorSwatch("palette.wooPurple.shade40", colors.palette.wooPurple.shade40, colors.onPrimary)
        MaterialProjectionSample()
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    color: Color,
    contentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color)
                .border(
                    width = DefaultWooStroke.regular,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.small,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MaterialProjectionSample() {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1)) {
        Text(
            text = "Material projection",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = WooTheme.text.labelSmall.regular,
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                modifier = Modifier.padding(WooTheme.padding.padding3),
                text = "MaterialTheme.colorScheme.primaryContainer",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TypographyFoundationSection() {
    val text = WooTheme.text

    FoundationSection(title = "Typography") {
        TypographySample("Display Large Regular", text.displayLarge.regular)
        TypographySample("Headline Small Emphasized", text.headlineSmall.emphasized)
        TypographySample("Title Medium Strong", text.titleMedium.strong)
        TypographySample("Body Medium Regular", text.bodyMedium.regular)
        TypographySample("Label Small Strong", text.labelSmall.strong)
        Text(
            text = "Font-family confirmation is pending; Android numeric type values are shown.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = text.bodySmall.regular,
        )
    }
}

@Composable
private fun TypographySample(
    label: String,
    style: TextStyle,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "Store management foundation",
            color = MaterialTheme.colorScheme.onBackground,
            style = style,
        )
    }
}

@Composable
private fun SpacingAndPaddingFoundationSection() {
    val spacing = WooTheme.spacing
    val padding = WooTheme.padding

    FoundationSection(title = "Spacing And Padding") {
        ScaleRows(
            prefix = "space",
            values = listOf(
                "0" to spacing.space0,
                "1" to spacing.space1,
                "2" to spacing.space2,
                "3" to spacing.space3,
                "4" to spacing.space4,
                "5" to spacing.space5,
                "6" to spacing.space6,
                "7" to spacing.space7,
                "8" to spacing.space8,
                "9" to spacing.space9,
                "10" to spacing.space10,
                "11" to spacing.space11,
                "12" to spacing.space12,
            ),
        )
        ScaleRows(
            prefix = "padding",
            values = listOf(
                "0" to padding.padding0,
                "1" to padding.padding1,
                "2" to padding.padding2,
                "3" to padding.padding3,
                "4" to padding.padding4,
                "5" to padding.padding5,
                "6" to padding.padding6,
                "7" to padding.padding7,
                "8" to padding.padding8,
                "9" to padding.padding9,
                "10" to padding.padding10,
                "11" to padding.padding11,
                "12" to padding.padding12,
            ),
        )
    }
}

@Composable
private fun ScaleRows(
    prefix: String,
    values: List<Pair<String, Dp>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        values.forEach { (label, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                Text(
                    modifier = Modifier.width(76.dp),
                    text = "$prefix$label",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Box(
                    modifier = Modifier
                        .width(value.coerceAtLeast(2.dp))
                        .height(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall,
                        ),
                )
                Text(
                    text = value.toString(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun RadiusFoundationSection() {
    val shapes = MaterialTheme.shapes

    FoundationSection(title = "Shapes") {
        RadiusSample("extraSmall", shapes.extraSmall)
        RadiusSample("small", shapes.small)
        RadiusSample("medium", shapes.medium)
        RadiusSample("large", shapes.large)
        RadiusSample("extraLarge", shapes.extraLarge)
    }
}

@Composable
private fun RadiusSample(label: String, shape: Shape) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = shape,
                ),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StrokeFoundationSection() {
    val stroke = DefaultWooStroke

    FoundationSection(title = "Internal Stroke") {
        StrokeSample("none", stroke.none)
        StrokeSample("extraThin", stroke.extraThin)
        StrokeSample("thin", stroke.thin)
        StrokeSample("regular", stroke.regular)
        StrokeSample("medium", stroke.medium)
        StrokeSample("mediumIncreased", stroke.mediumIncreased)
        StrokeSample("thick", stroke.thick)
        StrokeSample("extraThick", stroke.extraThick)
    }
}

@Composable
private fun StrokeSample(label: String, stroke: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = stroke,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small,
                ),
        )
        Text(
            text = "$label · $stroke",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OmittedFoundationSection() {
    FoundationSection(title = "Omitted In PR2") {
        Text(
            text = "Icon sizing remains probable/internal until the generic size source is accepted.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Elevation, minimum touch, and state-layer alpha primitives remain unsourced.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FoundationSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(WooTheme.spacing.space1))
        content()
    }
}
