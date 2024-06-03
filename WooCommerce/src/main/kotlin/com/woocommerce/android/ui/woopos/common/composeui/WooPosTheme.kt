@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.ui.compose.theme.WooTypography

private val DarkColorPalette = darkColors(
    primary = Color(0xFF1E88E5),
    primaryVariant = Color(0xFF1565C0),
    secondary = Color(0xFFD32F2F),
    onPrimary = Color.White,
    secondaryVariant = Color(0xFFB39DDB),
    surface = Color.White,
    onSurface = Color.Black,
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF1E88E5),
    primaryVariant = Color(0xFF1565C0),
    secondary = Color(0xFFD32F2F),
    onPrimary = Color.White,
    secondaryVariant = Color(0xFFB39DDB),
    surface = Color.Black,
    onSurface = Color.White
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
        content = content
    )
}
