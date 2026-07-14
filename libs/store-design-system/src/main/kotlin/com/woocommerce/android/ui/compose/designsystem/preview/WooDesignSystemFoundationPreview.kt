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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

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
    WooDesignSystemThemeWithBackground {
        content()
    }
}

@Composable
private fun FoundationPreviewContent() {
    val spacing = WooTheme.spacing

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(spacing.space5)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        ColorFoundationSection()
        TypographyFoundationSection()
        SpacingAndPaddingFoundationSection()
        RadiusFoundationSection()
        StrokeFoundationSection()
        IconSizeFoundationSection()
        OmittedFoundationSection()
    }
}

@Composable
private fun ColorFoundationSection() {
    val colors = WooTheme.colors

    FoundationSection(title = "Color") {
        ColorSwatch("primary", colors.primary, colors.onPrimary)
        ColorSwatch(
            label = "container.primaryContainer",
            color = colors.container.primaryContainer,
            contentColor = colors.container.onPrimaryContainer,
        )
        ColorSwatch("surface.default", colors.surface.default, colors.surface.onDefault)
        ColorSwatch("surface.surfaceDim", colors.surface.surfaceDim, colors.surface.onDefault)
        ColorSwatch(
            label = "surface.surfaceContainerHighest",
            color = colors.surface.surfaceContainerHighest,
            contentColor = colors.surface.onDefault,
        )
        ColorSwatch("surface.onVariantLowest", colors.surface.onVariantLowest, colors.surface.default)
        ColorSwatch("outlineVariant", colors.outlineVariant, colors.surface.onDefault)
        ColorSwatch("error", colors.error, colors.onError)
        ColorSwatch(
            label = "status.successContainer",
            color = colors.status.successContainer,
            contentColor = colors.status.onSuccessContainer,
        )
        ColorSwatch("alert.red", colors.alert.red, colors.alert.onRed)
        ColorSwatch("alert.orange", colors.alert.orange, colors.alert.onOrange)
        ColorSwatch("overlay.overlay20", colors.overlay.overlay20, colors.surface.default)
        ColorSwatch(
            "stateLayers.onSurface.opacity08",
            colors.stateLayers.onSurface.opacity08,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "stateLayers.onSurface.opacity10",
            colors.stateLayers.onSurface.opacity10,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "stateLayers.onSurface.opacity16",
            colors.stateLayers.onSurface.opacity16,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "stateLayers.onSurface.opacity24",
            colors.stateLayers.onSurface.opacity24,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "tintLayers.primaryContainer.opacity08",
            colors.tintLayers.primaryContainer.opacity08,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "tintLayers.primaryContainer.opacity10",
            colors.tintLayers.primaryContainer.opacity10,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "tintLayers.primaryContainer.opacity16",
            colors.tintLayers.primaryContainer.opacity16,
            colors.surface.onDefault,
        )
        ColorSwatch(
            "tintLayers.primaryContainer.opacity24",
            colors.tintLayers.primaryContainer.opacity24,
            colors.surface.onDefault,
        )
        ColorSwatch("palette.gray.shade40", colors.palette.gray.shade40, colors.onPrimary)
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
                    width = WooTheme.stroke.regular,
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
    val radius = WooTheme.radius

    FoundationSection(title = "Radius") {
        RadiusSample("none", radius.none)
        RadiusSample("extraSmall", radius.extraSmall)
        RadiusSample("small", radius.small)
        RadiusSample("medium", radius.medium)
        RadiusSample("large", radius.large)
        RadiusSample("extraLarge", radius.extraLarge)
        RadiusSample("full", radius.full)
    }
}

@Composable
private fun RadiusSample(label: String, radius: Dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(radius),
                ),
        )
        Text(
            text = "$label · $radius",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StrokeFoundationSection() {
    val stroke = WooTheme.stroke

    FoundationSection(title = "Stroke") {
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
private fun IconSizeFoundationSection() {
    val iconSize = WooTheme.iconSize

    FoundationSection(title = "Icon Size") {
        ScaleRows(
            prefix = "size",
            values = listOf(
                "14" to iconSize.size14,
                "16" to iconSize.size16,
                "18" to iconSize.size18,
                "20" to iconSize.size20,
                "24" to iconSize.size24,
                "32" to iconSize.size32,
            ),
        )
    }
}

@Composable
private fun OmittedFoundationSection() {
    FoundationSection(title = "Unresolved Foundations") {
        Text(
            text = "Elevation and minimum touch remain unsourced.",
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
