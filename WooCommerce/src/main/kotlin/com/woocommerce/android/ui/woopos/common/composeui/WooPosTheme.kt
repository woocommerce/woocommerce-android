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
    val paymentSuccessText: Color
)

private val DarkColorPalette = darkColors(
    primary = WooPOSColors.PrimaryDarkPurple,
    primaryVariant = WooPOSColors.PrimaryVariantDarkBlue,
    onPrimary = Color.White,
    secondary = WooPOSColors.SecondaryDarkGreen,
    secondaryVariant = WooPOSColors.SecondaryVariantDarkGray,
    surface = WooPOSColors.SurfaceDarkBlack,
    onSurface = Color.White,
    background = WooPOSColors.SurfaceDarkBlack,
    onBackground = Color.White
)

private val LightColorPalette = lightColors(
    primary = WooPOSColors.Purple50,
    primaryVariant = WooPOSColors.PrimaryVariantDarkBlue,
    onPrimary = Color.White,
    secondary = WooPOSColors.SecondaryLightGreen,
    secondaryVariant = WooPOSColors.SecondaryVariantLightGray,
    surface = Color.White,
    onSurface = Color.Black,
    background = WooPOSColors.Gray0,
    onBackground = Color.Black
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = WooPOSColors.DarkLoadingSkeleton,
    border = WooPOSColors.DarkBorder,
    success = WooPOSColors.DarkSuccess,
    error = WooPOSColors.DarkError,
    paymentSuccessBackground = WooPOSColors.DarkSuccessBackground,
    paymentSuccessText = WooPOSColors.DarkSuccessText
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = WooPOSColors.LightLoadingSkeleton,
    border = WooPOSColors.LightBorder,
    success = WooPOSColors.LightSuccess,
    error = WooPOSColors.LightError,
    paymentSuccessBackground = WooPOSColors.LightSuccessBackground,
    paymentSuccessText = WooPOSColors.Purple90
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
