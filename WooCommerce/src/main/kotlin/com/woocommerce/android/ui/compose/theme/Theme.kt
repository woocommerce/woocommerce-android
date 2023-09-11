package com.woocommerce.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable

/**
 * This theme should be used to support light/dark colors if the composable root of the view tree
 * does not support the use of contentColor.
 * @see <a href="https://developer.android.com/jetpack/compose/themes/material#content-color</a> for more details
 */
@Composable
fun WooThemeWithBackground(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    WooTheme(useDarkTheme) {
        SurfacedContent(content)
    }
}

@Composable
fun WooTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colors = colors,
        typography = WooTypography,
        content = content
    )
}

@Composable
private fun SurfacedContent(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        content()
    }
}

