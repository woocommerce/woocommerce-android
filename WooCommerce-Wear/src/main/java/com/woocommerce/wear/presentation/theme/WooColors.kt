package com.woocommerce.wear.presentation.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

object WooColors {
    val md_theme_light_primary = Color(0xFF674399)
    val md_theme_light_onPrimary = Color(0xFFFFFFFF)
    val md_theme_light_primary_variant = Color(0xFF3C2861)
    val md_theme_light_secondary = Color(0xFFC9356E)
    val md_theme_light_onSecondary = Color(0xFF000000)
    val md_theme_light_secondary_variant = Color(0xFF880E4F)
    val md_theme_light_error = Color(0xFFD63638)
    val md_theme_light_onError = Color(0xFFFFFFFF)
    val md_theme_light_background = Color(0xFFF2F2F2)
    val md_theme_light_onBackground = Color(0xFF000000)
    val md_theme_light_surface = Color(0xFFFFFFFF)
    val md_theme_light_onSurface = Color(0xFF000000)

    val md_theme_dark_primary = Color(0xFFB17FD4)
    val md_theme_dark_onPrimary = Color(0xFF000000)
    val md_theme_dark_primary_variant = Color(0xFF7F54B3)
    val md_theme_dark_secondary = Color(0xFFEB6594)
    val md_theme_dark_onSecondary = Color(0xFF000000)
    val md_theme_dark_secondary_variant = Color(0xFFC9356E)
    val md_theme_dark_error = Color(0xFFF86368)
    val md_theme_dark_onError = Color(0xFF000000)
    val md_theme_dark_background = Color(0xFF121212)
    val md_theme_dark_onBackground = Color(0xFFFFFFFF)
    val md_theme_dark_surface = Color(0xFF121212)
    val md_theme_dark_onSurface = Color(0xFFFFFFFF)
}

val LightColors = lightColors(
    primary = WooColors.md_theme_light_primary,
    primaryVariant = WooColors.md_theme_light_primary_variant,
    secondary = WooColors.md_theme_light_secondary,
    secondaryVariant = WooColors.md_theme_light_secondary_variant,
    background = WooColors.md_theme_light_background,
    surface = WooColors.md_theme_light_surface,
    error = WooColors.md_theme_light_error,
    onPrimary = WooColors.md_theme_light_onPrimary,
    onSecondary = WooColors.md_theme_light_onSecondary,
    onBackground = WooColors.md_theme_light_onBackground,
    onSurface = WooColors.md_theme_light_onSurface,
    onError = WooColors.md_theme_light_onError,
)

val DarkColors = darkColors(
    primary = WooColors.md_theme_dark_primary,
    primaryVariant = WooColors.md_theme_dark_primary_variant,
    secondary = WooColors.md_theme_dark_secondary,
    secondaryVariant = WooColors.md_theme_dark_secondary_variant,
    background = WooColors.md_theme_dark_background,
    surface = WooColors.md_theme_dark_surface,
    error = WooColors.md_theme_dark_error,
    onPrimary = WooColors.md_theme_dark_onPrimary,
    onSecondary = WooColors.md_theme_dark_onSecondary,
    onBackground = WooColors.md_theme_dark_onBackground,
    onSurface = WooColors.md_theme_dark_onSurface,
    onError = WooColors.md_theme_dark_onError,
)
