@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

data class CustomColors(
    val success: Color,
    val onSuccess: Color,

    val alert: Color = WooPosColors.Amber,
    val onAlert: Color = WooPosColors.White,

    val disabledContainer: Color,
    val onDisabledContainer: Color,

    val outline: Color,
    val outlineVariant: Color,

    val onSurfaceVariantLowest: Color,
    val onSurfaceVariantHighest: Color,

    val infoLowest: Color,
    val onInfoLowest: Color,

    val errorLowest: Color,
    val onErrorLowest: Color,

    val default: Color,
    val onDefault: Color,

    val tertiaryIconColor: Color,

    val transparent: Color = Color.Transparent,
    val unspecified: Color = Color.Unspecified,
)

private object WooPosColors {
    val WooPurple10 = Color(0xFFD1C1FF)
    val WooPurple40 = Color(0xFF873EFF)
    val WooPurple50 = Color(0xFF720EEC)
    val WooPurple80 = Color(0xFF3C087E)

    val WooRed0 = Color(0xFFF7EBEC)
    val WooRed10 = Color(0xFFFFABAF)
    val WooRed50 = Color(0xFFD63638)
    val WooRed30 = Color(0xFFF86368)
    val WooRed80 = Color(0xFF691C1C)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    val Amber = Color(0xFFF16618)

    val VividGreen = Color(0xFF1ED15A)
    val ForestGreen = Color(0xFF00A32A)

    val Gray0 = Color(0xFFF6F7F7)
    val Gray5 = Color(0xFFDCDCDE)
    val Gray20 = Color(0xFFA7AAAD)
    val Gray30 = Color(0xFF8C8F94)
    val Gray40 = Color(0xFF787C82)
    val Gray50 = Color(0xFF646970)
    val Gray60 = Color(0xFF50575E)
    val Gray70 = Color(0xFF3C434A)
    val Gray80 = Color(0xFF373A3E)
    val Gray90 = Color(0xFF1D2327)
    val Gray100 = Color(0xFF101517)

    val Blue10 = Color(0xFFD6DDF9)
    val Blue20 = Color(0xFFADBAF3)
    val Blue80 = Color(0xFF1F3286)
}

private val LightColorScheme = lightColorScheme(
    primary = WooPosColors.WooPurple40,
    onPrimary = Color.White,

    secondary = WooPosColors.WooPurple10,
    onSecondary = WooPosColors.Gray100,

    primaryContainer = WooPosColors.WooPurple40,
    onPrimaryContainer = WooPosColors.White,

    secondaryContainer = WooPosColors.Black,
    onSecondaryContainer = WooPosColors.White,

    surface = WooPosColors.Gray0,
    surfaceDim = WooPosColors.Gray0,
    surfaceBright = WooPosColors.White,

    surfaceContainerLowest = WooPosColors.White,
    surfaceContainerLow = WooPosColors.White,
    surfaceContainerHighest = WooPosColors.Gray30,

    onSurface = WooPosColors.Gray100,

    inverseSurface = WooPosColors.Gray100,
    inverseOnSurface = WooPosColors.White,

    error = WooPosColors.WooRed50,
    onError = WooPosColors.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = WooPosColors.WooPurple40,
    onPrimary = Color.White,

    secondary = WooPosColors.WooPurple10,
    onSecondary = WooPosColors.Gray100,

    primaryContainer = WooPosColors.WooPurple40,
    onPrimaryContainer = WooPosColors.White,

    secondaryContainer = WooPosColors.Black,
    onSecondaryContainer = WooPosColors.White,

    surface = WooPosColors.Gray100,
    surfaceDim = WooPosColors.Gray80,
    surfaceBright = WooPosColors.Gray90,

    surfaceContainerLowest = WooPosColors.Gray70,
    surfaceContainerLow = WooPosColors.Gray80,
    surfaceContainerHighest = WooPosColors.Gray100,

    onSurface = WooPosColors.White,

    inverseSurface = WooPosColors.White,
    inverseOnSurface = WooPosColors.Gray100,

    error = WooPosColors.WooRed30,
    onError = WooPosColors.Gray100,
)

private val LocalCustomColors = staticCompositionLocalOf {
    LightCustomColors
}

private val LightCustomColors = CustomColors(
    success = WooPosColors.ForestGreen,
    onSuccess = WooPosColors.White,

    disabledContainer = WooPosColors.Gray5,
    onDisabledContainer = WooPosColors.Gray20,

    outline = WooPosColors.Gray40,
    outlineVariant = WooPosColors.Gray5,

    onSurfaceVariantLowest = WooPosColors.Gray30,
    onSurfaceVariantHighest = WooPosColors.Gray60,

    infoLowest = WooPosColors.Blue10,
    errorLowest = WooPosColors.WooRed0,

    default = WooPosColors.Gray5,
    onDefault = WooPosColors.Gray100,

    onInfoLowest = WooPosColors.Blue80,
    onErrorLowest = WooPosColors.WooRed80,

    tertiaryIconColor = WooPosColors.WooPurple80
)

private val DarkCustomColors = CustomColors(
    success = WooPosColors.VividGreen,
    onSuccess = WooPosColors.Gray100,

    disabledContainer = WooPosColors.Gray60,
    onDisabledContainer = WooPosColors.Gray40,

    outline = WooPosColors.Gray30,
    outlineVariant = WooPosColors.Gray60,

    onSurfaceVariantLowest = WooPosColors.Gray50,
    onSurfaceVariantHighest = WooPosColors.Gray20,

    infoLowest = WooPosColors.Blue20,
    errorLowest = WooPosColors.WooRed10,

    default = WooPosColors.Gray5,
    onDefault = WooPosColors.Gray100,

    onInfoLowest = WooPosColors.Blue80,
    onErrorLowest = WooPosColors.WooRed80,

    tertiaryIconColor = WooPosColors.WooPurple50
)

@Composable
fun WooPosTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val customColors = if (isSystemInDarkTheme()) {
        DarkCustomColors
    } else {
        LightCustomColors
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(colorScheme = colorScheme) {
            SurfacedContent(content)
        }
    }
}

@Composable
private fun SurfacedContent(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.semantics { testTagsAsResourceId = true }) {
            content()
        }
    }
}

object WooPosTheme {
    val colors: CustomColors
        @Composable
        get() = LocalCustomColors.current
}
