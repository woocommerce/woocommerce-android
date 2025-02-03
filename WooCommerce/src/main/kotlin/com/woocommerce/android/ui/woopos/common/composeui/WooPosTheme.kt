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
    val totalsBackground: Color,
    val totalsErrorBackground: Color,
    val paymentSuccessBackground: Color,
    val paymentProcessingBackground: Color,
    val paymentSuccessText: Color,
    val paymentSuccessIcon: Color,
    val paymentProcessingText: Color,
    val dialogSubtitleHighlightBackground: Color = WooPosColors.lightQuaternaryBackground,
    val homeBackground: Color,
)

private object WooPosColors {
    // Woo POS specific colors:

    // Adding missing colors from the old code to match exactly
    val oldGrayLight = Color(0xFFF2EBFF)
    val oldGrayMedium = Color(0xFF8D8D8D)

    val primaryVariant = Color(0xFF3700B3)
    val secondary = Color(0xFF0A9400)
    val surface = Color(0xFF2E2E2E)

    // LightColorPalette
    val lightColorPaletteSecondary = Color(0xFF004B3E)
    val lightColorPaletteSecondaryVariant = Color(0xFF50575E)
    val lightColorPaletteBackground = Color(0xFFFDFDFD)

    // DarkCustomColors
    val darkCustomColorsError = Color(0xFFBE4400)
    val darkCustomloadingSkeleton = Color(0xFF616161)
    val darkCustomColorsHomeBackground = Color(0xFF1E1E1E)
    val darkQuaternaryBackground = Color(0xFF111111)
    val darkTotalsBackground = Color(0xFF1C1C1E)

    // LightCustomColors
    val lightCustomColorsError = Color(0xFFF16618)
    val lightCustomColorsLoadingSkeleton = Color(0xFFE1E1E1)
    val lightCustomColorsBorder = Color(0xFFC6C6C8)
    val lightQuaternaryBackground = Color(0x14747480)

    // Woo colors from here: https://color-studio.blog/
    val WooPurple0 = Color(0xFFF2EDFF)
    val WooPurple5 = Color(0xFFE1D7FF)
    val WooPurple10 = Color(0xFFD1C1FF)
    val WooPurple20 = Color(0xFFB999FF)
    val WooPurple30 = Color(0xFFA77EFF)
    val WooPurple40 = Color(0xFF873EFF)
    val WooPurple50 = Color(0xFF720EEC)
    val WooPurple60 = Color(0xFF6108CE)
    val WooPurple70 = Color(0xFF5007AA)
    val WooPurple80 = Color(0xFF3C087E)
    val WooPurple90 = Color(0xFF2C045D)
    val WooPurple100 = Color(0xFF1F0342)

    val Pink10 = Color(0xFFED9BB8)
    val Pink30 = Color(0xFFEB6594)
    val Pink50 = Color(0xFFC9356E)
    val Pink70 = Color(0xFF880E4F)
    val Pink90 = Color(0xFF5C0935)

    val Red5 = Color(0xFFFACFD2)
    val Red10 = Color(0xFFFFA6AB)
    val Red20 = Color(0xFFFF8085)
    val Red30 = Color(0xFFF86368)
    val Red50 = Color(0xFFD63638)
    val Red60 = Color(0xFFB32D2E)
    val Red70 = Color(0xFF8A2424)

    val Blue5 = Color(0xFFBBE0FA)
    val Blue30 = Color(0xFF5198D9)
    val Blue40 = Color(0xFF1689DB)
    val Blue50 = Color(0xFF2271B1)

    val Orange5 = Color(0xFFF7DCC6)
    val Orange10 = Color(0xFFFFBF86)
    val Orange30 = Color(0xFFE68B28)
    val Orange40 = Color(0xFFD67709)
    val Orange50 = Color(0xFFB26200)
    val Orange70 = Color(0xFF351F04)

    val Yellow10 = Color(0xFFF2CF75)
    val Yellow20 = Color(0xFFF0C443)
    val Yellow30 = Color(0xFFDBAE17)
    val Yellow50 = Color(0xFF907300)
    val Yellow70 = Color(0xFF5C4B00)

    val Celadon0 = Color(0xFFECF7F4)
    val Celadon5 = Color(0xFFA7E8D4)
    val Celadon10 = Color(0xFF65D9B9)
    val Celadon20 = Color(0xFF2FC39E)
    val Celadon40 = Color(0xFF009172)

    val Green0 = Color(0xFFEBF7F1)
    val Green5 = Color(0xFFA4F5C8)
    val Green10 = Color(0xFF59E38F)
    val Green20 = Color(0xFF1ED15A)
    val Green50 = Color(0xFF008A20)

    val emerald20 = Color(0xFF98F179)
    val emerald60 = Color(0xFF028C59)

    val White = Color(0xFFFFFFFF)

    val Gray0 = Color(0xFFF6F7F7)
    val Gray5 = Color(0xFFDCDCDE)
    val Gray20 = Color(0xFFB4B1B8)
    val Gray40 = Color(0xFF787C82)
    val Gray60 = Color(0xFF51565F)
    val Gray70 = Color(0xFF3D444B)
    val Gray80 = Color(0xFF2C3338)
    val Gray900 = Color(0xFFF7F7F7)

    val Black = Color(0xFF000000)
    val Black60 = Color(0xFF6A6A6A)
    val Black80 = Color(0xFF363636)
    val Black90 = Color(0xFF121212)
}

private val DarkColorPalette = darkColors(
    primary = WooPosColors.WooPurple30,
    primaryVariant = WooPosColors.primaryVariant,
    onPrimary = Color.Black,
    secondary = WooPosColors.secondary,
    secondaryVariant = WooPosColors.oldGrayMedium,
    surface = WooPosColors.surface,
    onSurface = Color.White,
    background = WooPosColors.Black90,
    onBackground = Color.White,
)

private val LightColorPalette = lightColors(
    primary = WooPosColors.WooPurple40,
    primaryVariant = WooPosColors.primaryVariant,
    onPrimary = Color.White,
    secondary = WooPosColors.lightColorPaletteSecondary,
    secondaryVariant = WooPosColors.lightColorPaletteSecondaryVariant,
    surface = Color.White,
    onSurface = Color.Black,
    background = WooPosColors.lightColorPaletteBackground,
    onBackground = Color.Black,
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = WooPosColors.darkCustomloadingSkeleton,
    border = WooPosColors.oldGrayMedium,
    success = WooPosColors.emerald60,
    error = WooPosColors.darkCustomColorsError,
    totalsErrorBackground = WooPosColors.darkQuaternaryBackground,
    totalsBackground = WooPosColors.darkTotalsBackground,
    paymentSuccessBackground = WooPosColors.darkCustomColorsHomeBackground,
    paymentSuccessText = WooPosColors.oldGrayLight,
    paymentSuccessIcon = WooPosColors.darkCustomColorsHomeBackground,
    paymentProcessingText = WooPosColors.White,
    homeBackground = WooPosColors.darkCustomColorsHomeBackground,
    paymentProcessingBackground = WooPosColors.WooPurple70,
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = WooPosColors.lightCustomColorsLoadingSkeleton,
    border = WooPosColors.lightCustomColorsBorder,
    success = WooPosColors.emerald20,
    error = WooPosColors.lightCustomColorsError,
    totalsErrorBackground = WooPosColors.lightQuaternaryBackground,
    totalsBackground = WooPosColors.Gray0,
    paymentSuccessBackground = WooPosColors.White,
    paymentSuccessText = WooPosColors.WooPurple90,
    paymentProcessingText = WooPosColors.White,
    paymentSuccessIcon = Color.White,
    homeBackground = WooPosColors.Gray0,
    paymentProcessingBackground = WooPosColors.WooPurple70,
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
        @Composable get() = LocalCustomColors.current
}
