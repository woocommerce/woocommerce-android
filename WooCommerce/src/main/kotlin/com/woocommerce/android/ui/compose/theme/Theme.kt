package com.woocommerce.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.material.MaterialTheme as Material2Theme
import androidx.compose.material.Surface as Material2Surface

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
    val material2Colors = if (!useDarkTheme) {
        Material2LightColors
    } else {
        Material2DarkColors
    }

    val material3Colors = if (!useDarkTheme) {
        Material3LightColorScheme
    } else {
        Material3DarkColorScheme
    }

    MaterialTheme(
        colorScheme = material3Colors,
        shapes = MaterialTheme.shapes.copy(
            small = Material2Theme.shapes.small,
            medium = Material2Theme.shapes.medium,
            large = Material2Theme.shapes.large
        ),
        typography = Material3Typography
    ) {
        Material2Theme(
            colors = material2Colors,
            typography = Material2Typography,
            content = content
        )
    }
}

@Composable
private fun SurfacedContent(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Material2Surface(color = Material2Theme.colors.background) {
            content()
        }
    }
}
