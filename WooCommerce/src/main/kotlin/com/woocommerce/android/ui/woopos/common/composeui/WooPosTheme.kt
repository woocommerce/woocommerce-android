@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.ui.compose.theme.WooTypography

private val DarkColorPalette = darkColors(
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC5),
    secondaryVariant = Color(0xFF8D8D8D),
    onPrimary = Color.Black,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF6200EA),
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF004B3E),
    secondaryVariant = Color(0xFF50575E),
    onPrimary = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    background = Color(0xFFF6F7F7),
    onBackground = Color.Black,
)

@Composable
fun WooPosTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = WooTypography,
    ) {
        SurfacedContent(content)
    }
}

@Composable
private fun SurfacedContent(
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        content()
    }
}
