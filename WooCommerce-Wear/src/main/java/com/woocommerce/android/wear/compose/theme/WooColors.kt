package com.woocommerce.android.wear.compose.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

@Suppress("MagicNumber")
object WooColors {
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
    val md_theme_dark_surface = Color(0xFF000000)
    val md_theme_dark_onSurface = Color(0xFFFFFFFF)
    val woo_purple_surface = Color(0xBB533582)
    val woo_purple_5 = Color(0xFFDFD1FB)
    val woo_purple_10 = Color(0xFFCFB9F6)
    val woo_purple_20 = Color(0xFFBEA0F2)
    val woo_purple_alpha = Color(0x66674399)
    val woo_gray_alpha = Color(0x80FFFFFF)
    val woo_amber_40 = Color(0xFFFFA60E)
}

val WooWearColors = Colors(
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
