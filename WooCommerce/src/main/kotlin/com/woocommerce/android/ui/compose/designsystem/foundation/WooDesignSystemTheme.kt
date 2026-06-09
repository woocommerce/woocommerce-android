package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.woocommerce.android.ui.compose.designsystem.WooTopAppBarAppearance

@Composable
fun WooDesignSystemTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = wooColors(useDarkTheme)
    val typography = DefaultWooTypography
    val radius = DefaultWooRadius

    ProvideWooFoundation(
        colors = colors,
        typography = typography,
        topAppBarAppearance = WooTopAppBarAppearance.DesignSystem,
        content = {
            MaterialTheme(
                colorScheme = colors.toMaterialColorScheme(),
                typography = typography.toMaterialTypography(),
                shapes = radius.toMaterialShapes(),
            ) {
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    content()
                }
            }
        },
    )
}

@Composable
fun WooDesignSystemThemeWithBackground(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    WooDesignSystemTheme(useDarkTheme = useDarkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
internal fun LegacyWooFoundation(content: @Composable () -> Unit) {
    // Installs DS locals without replacing the legacy MaterialTheme projection.
    ProvideWooFoundation(
        colors = legacyWooColors(),
        typography = LegacyWooTypography,
        topAppBarAppearance = WooTopAppBarAppearance.LegacyCompatible,
        content = content,
    )
}

@Composable
private fun ProvideWooFoundation(
    colors: WooColors,
    typography: WooTypography,
    spacing: WooSpacing = DefaultWooSpacing,
    padding: WooPadding = DefaultWooPadding,
    topAppBarAppearance: WooTopAppBarAppearance = WooTopAppBarAppearance.DesignSystem,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWooColors provides colors,
        LocalWooText provides typography,
        LocalWooSpacing provides spacing,
        LocalWooPadding provides padding,
        LocalWooTopAppBarAppearance provides topAppBarAppearance,
    ) {
        content()
    }
}

internal val LocalWooSpacing = staticCompositionLocalOf<WooSpacing> {
    error("WooTheme.spacing is not available. Wrap content in WooDesignSystemTheme or WooThemeWithBackground.")
}

internal val LocalWooPadding = staticCompositionLocalOf<WooPadding> {
    error("WooTheme.padding is not available. Wrap content in WooDesignSystemTheme or WooThemeWithBackground.")
}

internal val LocalWooTopAppBarAppearance = staticCompositionLocalOf<WooTopAppBarAppearance> {
    error(
        "WooTheme.topAppBarAppearance is not available. Wrap content in WooDesignSystemTheme or " +
            "WooThemeWithBackground."
    )
}
