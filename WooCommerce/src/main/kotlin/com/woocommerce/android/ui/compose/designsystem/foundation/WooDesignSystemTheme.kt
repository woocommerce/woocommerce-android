package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
fun WooDesignSystemTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = wooColors(useDarkTheme)
    val typography = DefaultWooTypography
    val radius = DefaultWooRadius

    CompositionLocalProvider(
        LocalWooColors provides colors,
        LocalWooText provides typography,
        LocalWooSpacing provides DefaultWooSpacing,
        LocalWooPadding provides DefaultWooPadding,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = radius.toMaterialShapes(),
        ) {
            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                content()
            }
        }
    }
}

internal val LocalWooSpacing = staticCompositionLocalOf {
    DefaultWooSpacing
}

internal val LocalWooPadding = staticCompositionLocalOf {
    DefaultWooPadding
}
