package com.woocommerce.android.ui.compose.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.woocommerce.android.R

// Material 2 colors for backward compatibility
val Material2LightColors
    @Composable
    get() = lightColors(
        primary = colorResource(R.color.color_primary),
        primaryVariant = colorResource(R.color.color_primary_variant),
        secondary = colorResource(R.color.color_secondary),
        secondaryVariant = colorResource(R.color.color_secondary_variant),
        background = colorResource(R.color.default_window_background),
        surface = colorResource(R.color.color_surface),
        error = colorResource(R.color.color_error),
        onPrimary = colorResource(R.color.color_on_primary),
        onSecondary = colorResource(R.color.color_on_secondary),
        onBackground = colorResource(R.color.color_on_background),
        onSurface = colorResource(R.color.color_on_surface),
        onError = colorResource(R.color.color_on_error),
    )

val Material2DarkColors
    @Composable
    get() = darkColors(
        primary = colorResource(R.color.color_primary),
        primaryVariant = colorResource(R.color.color_primary_variant),
        secondary = colorResource(R.color.color_secondary),
        secondaryVariant = colorResource(R.color.color_secondary_variant),
        background = colorResource(R.color.default_window_background),
        surface = colorResource(R.color.color_surface),
        error = colorResource(R.color.color_error),
        onPrimary = colorResource(R.color.color_on_primary),
        onSecondary = colorResource(R.color.color_on_secondary),
        onBackground = colorResource(R.color.color_on_background),
        onSurface = colorResource(R.color.color_on_surface),
        onError = colorResource(R.color.color_on_error),
    )

// Material 3 color schemes
val Material3LightColorScheme
    @Composable
    get() = lightColorScheme(
        primary = colorResource(R.color.color_primary),
        onPrimary = colorResource(R.color.color_on_primary),
        primaryContainer = colorResource(R.color.color_primary),
        onPrimaryContainer = colorResource(R.color.color_on_primary),
        secondary = colorResource(R.color.color_secondary),
        onSecondary = colorResource(R.color.color_on_secondary),
        secondaryContainer = colorResource(R.color.color_secondary),
        onSecondaryContainer = colorResource(R.color.color_on_secondary),
        tertiary = colorResource(R.color.color_secondary),
        onTertiary = colorResource(R.color.color_on_secondary),
        tertiaryContainer = colorResource(R.color.color_secondary),
        onTertiaryContainer = colorResource(R.color.color_on_secondary),
        error = colorResource(R.color.color_error),
        onError = colorResource(R.color.color_on_error),
        errorContainer = colorResource(R.color.color_error),
        onErrorContainer = colorResource(R.color.color_on_error),
        background = colorResource(R.color.default_window_background),
        onBackground = colorResource(R.color.color_on_background),
        surface = colorResource(R.color.color_surface),
        onSurface = colorResource(R.color.color_on_surface),
        surfaceVariant = colorResource(R.color.color_surface_variant),
        onSurfaceVariant = colorResource(R.color.color_on_surface_medium),
        outline = colorResource(R.color.divider_color),
        inverseOnSurface = colorResource(R.color.color_surface),
        inverseSurface = colorResource(R.color.color_on_surface),
        inversePrimary = colorResource(R.color.color_on_primary),
        surfaceBright = colorResource(R.color.color_surface),
        surfaceContainer = colorResource(R.color.color_surface_elevated),
        surfaceContainerHigh = colorResource(R.color.color_surface_elevated),
        surfaceContainerHighest = colorResource(R.color.color_surface_elevated),
        surfaceContainerLow = colorResource(R.color.color_surface_elevated),
        surfaceContainerLowest = colorResource(R.color.color_surface_elevated),
    )

val Material3DarkColorScheme
    @Composable
    get() = darkColorScheme(
        primary = colorResource(R.color.color_primary),
        onPrimary = colorResource(R.color.color_on_primary),
        primaryContainer = colorResource(R.color.color_primary),
        onPrimaryContainer = colorResource(R.color.color_on_primary),
        secondary = colorResource(R.color.color_secondary),
        onSecondary = colorResource(R.color.color_on_secondary),
        secondaryContainer = colorResource(R.color.color_secondary),
        onSecondaryContainer = colorResource(R.color.color_on_secondary),
        tertiary = colorResource(R.color.color_secondary),
        onTertiary = colorResource(R.color.color_on_secondary),
        tertiaryContainer = colorResource(R.color.color_secondary),
        onTertiaryContainer = colorResource(R.color.color_on_secondary),
        error = colorResource(R.color.color_error),
        onError = colorResource(R.color.color_on_error),
        errorContainer = colorResource(R.color.color_error),
        onErrorContainer = colorResource(R.color.color_on_error),
        background = colorResource(R.color.default_window_background),
        onBackground = colorResource(R.color.color_on_background),
        surface = colorResource(R.color.color_surface),
        onSurface = colorResource(R.color.color_on_surface),
        surfaceVariant = colorResource(R.color.color_surface_variant),
        onSurfaceVariant = colorResource(R.color.color_on_surface_medium),
        outline = colorResource(R.color.divider_color),
        inverseOnSurface = colorResource(R.color.color_surface),
        inverseSurface = colorResource(R.color.color_on_surface),
        inversePrimary = colorResource(R.color.color_on_primary),
        surfaceBright = colorResource(R.color.color_surface),
        surfaceContainer = colorResource(R.color.color_surface_elevated_04),
        surfaceContainerHigh = colorResource(R.color.color_surface_elevated),
        surfaceContainerHighest = colorResource(R.color.color_surface_elevated),
        surfaceContainerLow = colorResource(R.color.color_elevation_overlay_01),
        surfaceContainerLowest = colorResource(R.color.color_elevation_overlay_01),
    )
