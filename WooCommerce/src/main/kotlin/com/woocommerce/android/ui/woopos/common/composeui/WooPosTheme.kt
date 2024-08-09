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
    onSurface = Color.White,
    background = Color(0xFF121212),
    onBackground = WooColors.White,
)

private val LightColorPalette = lightColors(
    primary = WooColors.Purple50,
    primaryVariant = Color(0xFF3700B3),
    onPrimary = WooColors.White,
    secondary = Color(0xFF004B3E),
    secondaryVariant = Color(0xFF50575E),
    surface = WooColors.White,
    onSurface = WooColors.Black,
    background = WooColors.Gray0,
    onBackground = WooColors.Black
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = WooColors.Gray60,
    border = WooColors.Gray40,
    success = WooColors.Green20,
    error = WooColors.Red50,
    paymentSuccessBackground = WooColors.Green10,
    paymentSuccessText = WooColors.White,
    wooPurple0 = WooColors.Purple90,
    wooPurple10 = WooColors.Purple80,
    wooPurple15 = WooColors.Purple60,
    wooPurple20 = WooColors.Purple50,
    wooPurple30 = WooColors.Purple40,
    wooPurple40 = WooColors.Purple40,
    wooPurple50 = WooColors.Purple30,
    wooPurple60 = WooColors.Purple20,
    wooPurple80 = WooColors.Purple10,
    wooPurple90 = WooColors.Purple0,
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = WooColors.Gray60,
    border = WooColors.Gray40,
    success = WooColors.Green20,
    error = WooColors.Red50,
    paymentSuccessBackground = WooColors.Green10,
    paymentSuccessText = WooColors.White,
    wooPurple0 = WooColors.Purple90,
    wooPurple10 = WooColors.Purple80,
    wooPurple15 = WooColors.Purple60,
    wooPurple20 = WooColors.Purple50,
    wooPurple30 = WooColors.Purple40,
    wooPurple40 = WooColors.Purple40,
    wooPurple50 = WooColors.Purple30,
    wooPurple60 = WooColors.Purple20,
    wooPurple80 = WooColors.Purple10,
    wooPurple90 = WooColors.Purple0,
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
