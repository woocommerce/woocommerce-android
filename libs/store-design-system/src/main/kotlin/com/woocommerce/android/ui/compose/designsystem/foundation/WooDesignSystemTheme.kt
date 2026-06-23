package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

@Composable
fun WooDesignSystemTheme(
    foundation: WooFoundation = WooFoundationDefaults.foundation(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = foundation.colors.toMaterialColorScheme(),
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
    foundation: WooFoundation = WooFoundationDefaults.foundation(),
    content: @Composable () -> Unit,
) {
    WooDesignSystemTheme(foundation = foundation) {
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
    ) {
        content()
    }
}

internal val LocalWooSpacing = staticCompositionLocalOf<WooSpacing> {
    error("WooTheme.spacing is not available. Wrap content in WooDesignSystemTheme.")
}

internal val LocalWooPadding = staticCompositionLocalOf<WooPadding> {
    error("WooTheme.padding is not available. Wrap content in WooDesignSystemTheme.")
}

internal val LocalWooRadius = staticCompositionLocalOf<WooRadius> {
    error("WooTheme.radius is not available. Wrap content in WooDesignSystemTheme.")
}
