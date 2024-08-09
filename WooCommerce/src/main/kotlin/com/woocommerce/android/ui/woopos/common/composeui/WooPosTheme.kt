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
    val wooPurple0: Color,
    val wooPurple10: Color,
    val wooPurple15: Color,
    val wooPurple20: Color,
    val wooPurple30: Color,
    val wooPurple40: Color,
    val wooPurple50: Color,
    val wooPurple60: Color,
    val wooPurple80: Color,
    val wooPurple90: Color,
)

private val DarkColorPalette = darkColors(
    primary = Color(0xFF9C70D3),
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color.White,
    secondary = Color(0xFF0A9400),
    secondaryVariant = Color(0xFF8D8D8D),
    surface = Color(0xFF121212),
    onSurface = WooPOSColors.White,
    background = Color(0xFF121212),
    onBackground = WooPOSColors.White,
)

private val LightColorPalette = lightColors(
    primary = WooPOSColors.Purple50,
    primaryVariant = Color(0xFF3700B3),
    onPrimary = WooPOSColors.White,
    secondary = Color(0xFF004B3E),
    secondaryVariant = Color(0xFF50575E),
    surface = WooPOSColors.White,
    onSurface = WooPOSColors.Black,
    background = WooPOSColors.Gray0,
    onBackground = WooPOSColors.Black
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = Color(0xFF616161),
    border = Color(0xFF8D8D8D),
    success = Color(0xFF06B166),
    error = Color(0xFFBE4400),
    paymentSuccessBackground = Color(0xFF74C758),
    paymentSuccessText = Color(0xFFF2EBFF),
    wooPurple0 = WooPOSColors.Purple0,
    wooPurple10 = WooPOSColors.Purple10,
    wooPurple15 = WooPOSColors.Purple15,
    wooPurple20 = WooPOSColors.Purple20,
    wooPurple30 = WooPOSColors.Purple30,
    wooPurple40 = WooPOSColors.Purple40,
    wooPurple50 = WooPOSColors.Purple40,
    wooPurple60 = WooPOSColors.Purple60,
    wooPurple80 = WooPOSColors.Purple80,
    wooPurple90 = WooPOSColors.Purple90,
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = Color(0xFFE1E1E1),
    border = Color(0xFFC6C6C8),
    success = Color(0xFF03D479),
    error = Color(0xFFF16618),
    paymentSuccessBackground = Color(0xFF98F179),
    paymentSuccessText = WooPOSColors.Purple90,
    wooPurple0 = WooPOSColors.Purple0,
    wooPurple10 = WooPOSColors.Purple10,
    wooPurple15 = WooPOSColors.Purple15,
    wooPurple20 = WooPOSColors.Purple20,
    wooPurple30 = WooPOSColors.Purple30,
    wooPurple40 = WooPOSColors.Purple40,
    wooPurple50 = WooPOSColors.Purple40,
    wooPurple60 = WooPOSColors.Purple60,
    wooPurple80 = WooPOSColors.Purple80,
    wooPurple90 = WooPOSColors.Purple90,
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
