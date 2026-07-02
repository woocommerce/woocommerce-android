package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
fun WooDesignSystemTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    foundation: WooFoundation = WooFoundationDefaults.foundation(useDarkTheme = useDarkTheme),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = foundation.colors.toMaterialColorScheme(useDarkTheme = useDarkTheme),
        typography = foundation.text.toMaterialTypography(),
        shapes = foundation.radius.toMaterialShapes(),
    ) {
        ProvideWooDesignSystemFoundation(foundation = foundation) {
            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                content()
            }
        }
    }
}

@Composable
fun WooDesignSystemThemeWithBackground(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    foundation: WooFoundation = WooFoundationDefaults.foundation(useDarkTheme = useDarkTheme),
    content: @Composable () -> Unit,
) {
    WooDesignSystemTheme(
        useDarkTheme = useDarkTheme,
        foundation = foundation,
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
internal fun ProvideWooDesignSystemFoundation(
    foundation: WooFoundation,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWooColors provides foundation.colors,
        LocalWooText provides foundation.text,
        LocalWooSpacing provides foundation.spacing,
        LocalWooPadding provides foundation.padding,
        LocalWooRadius provides foundation.radius,
        LocalWooIconSize provides foundation.iconSize,
        LocalWooStroke provides foundation.stroke,
    ) {
        content()
    }
}
