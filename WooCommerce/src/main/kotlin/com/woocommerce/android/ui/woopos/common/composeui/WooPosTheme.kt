@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.ui.compose.theme.WooTypography

data class CustomColors(
    val loadingSkeleton: Color,
    val border: Color,
    val success: Color,
    val error: Color,
    val paymentSuccessBackground: Color,
    val paymentSuccessText: Color,
    val paymentSuccessIcon: Color,
    val paymentSuccessIconBackground: Color,
    val dialogSubtitleHighlightBackground: Color = Color(0x14747480),
)

private val DarkColorPalette = darkColors(
    primary = Color(0xFF9C70D3),
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color.Black,
    secondary = Color(0xFF0A9400),
    secondaryVariant = Color(0xFF8D8D8D),
    surface = Color(0xFF2E2E2E),
    onSurface = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF7F54B3),
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color.White,
    secondary = Color(0xFF004B3E),
    secondaryVariant = Color(0xFF50575E),
    surface = Color.White,
    onSurface = Color.Black,
    background = Color(0xFFFDFDFD),
    onBackground = Color.Black,
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = Color(0xFF616161),
    border = Color(0xFF8D8D8D),
    success = Color(0xFF06B166),
    error = Color(0xFFBE4400),
    paymentSuccessBackground = Color(0xFF005139),
    paymentSuccessText = Color(0xFFF2EBFF),
    paymentSuccessIcon = Color.White,
    paymentSuccessIconBackground = Color(0xFF00AD64)
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = Color(0xFFE1E1E1),
    border = Color(0xFFC6C6C8),
    success = Color(0xFF03D479),
    error = Color(0xFFF16618),
    paymentSuccessBackground = Color(0xFF98F179),
    paymentSuccessText = Color(0xFF271B3D),
    paymentSuccessIcon = Color(0xFF03D479),
    paymentSuccessIconBackground = Color.White,
)

private val LocalCustomColors = staticCompositionLocalOf {
    LightCustomColors
}

@Composable
fun WooPosTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val customColors = if (isSystemInDarkTheme()) {
        DarkCustomColors
    } else {
        LightCustomColors
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colors = colors,
            typography = WooTypography,
        ) {
            SurfacedContent(content)
        }
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

object WooPosTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
