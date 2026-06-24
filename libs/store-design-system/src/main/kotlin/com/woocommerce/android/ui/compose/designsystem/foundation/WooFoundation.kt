package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
data class WooFoundation(
    val colors: WooColors,
    val text: WooTypography,
    val spacing: WooSpacing,
    val padding: WooPadding,
    val radius: WooRadius,
    val iconSize: WooIconSize,
)

object WooFoundationDefaults {
    val palette: WooPaletteColors = FixedWooPaletteColors
    val text: WooTypography = DefaultWooTypography
    val spacing: WooSpacing = DefaultWooSpacing
    val padding: WooPadding = DefaultWooPadding
    val radius: WooRadius = DefaultWooRadius
    val iconSize: WooIconSize = DefaultWooIconSize

    @Composable
    fun foundation(useDarkTheme: Boolean = isSystemInDarkTheme()): WooFoundation = WooFoundation(
        colors = colors(useDarkTheme = useDarkTheme),
        text = text,
        spacing = spacing,
        padding = padding,
        radius = radius,
        iconSize = iconSize,
    )

    @Composable
    fun colors(useDarkTheme: Boolean): WooColors = wooColors(useDarkTheme = useDarkTheme)
}
