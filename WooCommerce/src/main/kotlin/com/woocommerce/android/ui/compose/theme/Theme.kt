package com.woocommerce.android.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.android.material.composethemeadapter.createMdcTheme

/**
 * This theme should be used to support light/dark colors if the composable root of the view tree
 * does not support the use of contentColor.
 * @see <a href="https://developer.android.com/jetpack/compose/themes/material#content-color</a> for more details
 */
@Composable
fun WooThemeWithBackground(
    content: @Composable () -> Unit
) {
    WooTheme {
        SurfacedContent(content)
    }
}

@Composable
fun WooTheme(
    content: @Composable () -> Unit
) {
    val (colors, typography, shapes) = createMdcTheme(
        context = LocalContext.current,
        layoutDirection = LocalLayoutDirection.current
    )

    BaseWooTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}

@Composable
private fun BaseWooTheme(
    colors: Colors? = null,
    typography: Typography? = null,
    shapes: Shapes? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors ?: MaterialTheme.colors,
        typography = typography ?: MaterialTheme.typography,
        shapes = shapes ?: MaterialTheme.shapes,
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

@Composable
internal fun WooThemePreview(
    lightColors: Colors? = lightColors(),
    darkColors: Colors? = darkColors(),
    typography: Typography? = null,
    shapes: Shapes? = null,
    content: @Composable () -> Unit
) {
    BaseWooTheme(
        colors = (if (isSystemInDarkTheme()) darkColors else lightColors),
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
