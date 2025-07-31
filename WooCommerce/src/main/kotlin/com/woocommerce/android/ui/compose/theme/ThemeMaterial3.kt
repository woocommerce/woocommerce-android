package com.woocommerce.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = WooColors.md_theme_light_primary,
    onPrimary = WooColors.md_theme_light_onPrimary,
    primaryContainer = WooColors.md_theme_light_primary_variant,
    onPrimaryContainer = WooColors.md_theme_light_onPrimary,

    secondary = WooColors.md_theme_light_secondary,
    onSecondary = WooColors.md_theme_light_onSecondary,
    secondaryContainer = WooColors.md_theme_light_secondary_variant,
    onSecondaryContainer = WooColors.md_theme_light_onSecondary,

    tertiary = Color(0xFF0066CC),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F0FF),
    onTertiaryContainer = Color(0xFF00264D),

    error = WooColors.md_theme_light_error,
    onError = WooColors.md_theme_light_onError,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = WooColors.md_theme_light_background,
    onBackground = WooColors.md_theme_light_onBackground,

    surface = WooColors.md_theme_light_surface,
    onSurface = WooColors.md_theme_light_onSurface,
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = WooColors.md_theme_dark_primary
)

private val DarkColorScheme = darkColorScheme(
    primary = WooColors.md_theme_dark_primary,
    onPrimary = WooColors.md_theme_dark_onPrimary,
    primaryContainer = WooColors.md_theme_dark_primary_variant,
    onPrimaryContainer = Color(0xFFEADDFF),

    secondary = WooColors.md_theme_dark_secondary,
    onSecondary = WooColors.md_theme_dark_onSecondary,
    secondaryContainer = WooColors.md_theme_dark_secondary_variant,
    onSecondaryContainer = Color(0xFFFFD8E4),

    tertiary = Color(0xFF99CCFF),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF004880),
    onTertiaryContainer = Color(0xFFD1E4FF),

    error = WooColors.md_theme_dark_error,
    onError = WooColors.md_theme_dark_onError,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = WooColors.md_theme_dark_background,
    onBackground = WooColors.md_theme_dark_onBackground,

    surface = WooColors.md_theme_dark_surface,
    onSurface = WooColors.md_theme_dark_onSurface,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = WooColors.md_theme_light_primary
)

@Composable
fun WooThemeM3(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (!useDarkTheme) {
        LightColorScheme
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WooTypographyM3,
        content = content
    )
}
