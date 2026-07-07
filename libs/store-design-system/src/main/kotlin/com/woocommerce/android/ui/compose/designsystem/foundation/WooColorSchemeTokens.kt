package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

internal fun WooColors.toMaterialColorScheme(useDarkTheme: Boolean): ColorScheme =
    if (useDarkTheme) {
        toDarkMaterialColorScheme()
    } else {
        toLightMaterialColorScheme()
    }

private fun WooColors.toDarkMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = container.primaryContainer,
    onPrimaryContainer = container.onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = container.secondaryContainer,
    onSecondaryContainer = container.onSecondaryContainer,
    tertiary = secondary,
    onTertiary = onSecondary,
    tertiaryContainer = container.secondaryContainer,
    onTertiaryContainer = container.onSecondaryContainer,
    background = background.section,
    onBackground = background.onSection,
    surface = surface.default,
    onSurface = surface.onDefault,
    surfaceVariant = background.sectionVariant,
    onSurfaceVariant = surface.onVariant,
    surfaceBright = surface.default,
    surfaceDim = surface.surfaceDim,
    surfaceContainer = background.section,
    surfaceContainerHigh = surface.default,
    surfaceContainerHighest = surface.surfaceContainerHighest,
    surfaceContainerLow = background.sectionVariant,
    surfaceContainerLowest = surface.default,
    inverseSurface = surface.inverted,
    inverseOnSurface = surface.onInverted,
    errorContainer = status.errorContainer,
    onErrorContainer = status.onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = overlay.overlay50,
)

private fun WooColors.toLightMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = container.primaryContainer,
    onPrimaryContainer = container.onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = container.secondaryContainer,
    onSecondaryContainer = container.onSecondaryContainer,
    tertiary = secondary,
    onTertiary = onSecondary,
    tertiaryContainer = container.secondaryContainer,
    onTertiaryContainer = container.onSecondaryContainer,
    background = background.section,
    onBackground = background.onSection,
    surface = surface.default,
    onSurface = surface.onDefault,
    surfaceVariant = background.sectionVariant,
    onSurfaceVariant = surface.onVariant,
    surfaceBright = surface.default,
    surfaceDim = surface.surfaceDim,
    surfaceContainer = background.section,
    surfaceContainerHigh = surface.default,
    surfaceContainerHighest = surface.surfaceContainerHighest,
    surfaceContainerLow = background.sectionVariant,
    surfaceContainerLowest = surface.default,
    inverseSurface = surface.inverted,
    inverseOnSurface = surface.onInverted,
    errorContainer = status.errorContainer,
    onErrorContainer = status.onErrorContainer,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = overlay.overlay50,
)
