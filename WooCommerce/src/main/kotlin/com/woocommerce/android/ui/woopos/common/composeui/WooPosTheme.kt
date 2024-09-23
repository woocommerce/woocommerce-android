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
    val homeBackground: Color,
)

private object WooPosColors {
    // Woo POS specific colors:

    // Adding missing colors from the old code to match exactly
    val primary = Color(0xFF9C70D3)
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
    val darkCustomColorsPaymentSuccessBackground = Color(0xFF005139)
    val darkCustomColorsPaymentSuccessIconBackground = Color(0xFF00AD64)
    val darkCustomloadingSkeleton = Color(0xFF616161)
    val darkCustomColorsSuccess = Color(0xFF06B166)
    val darkCustomColorsHomeBackground = Color(0xFF1E1E1E)

    // LightCustomColors
    val lightCustomColorsError = Color(0xFFF16618)
    val lightCustomColorsPaymentSuccessBackground = Color(0xFF98F179)
    val lightCustomColorsSuccess = Color(0xFF03D479)
    val lightCustomColorsLoadingSkeleton = Color(0xFFE1E1E1)
    val lightCustomColorsBorder = Color(0xFFC6C6C8)

    // Woo colors from here: W5OBIbzWilNI8qely8Y4OHQd-fi-144_2
    val WooPurple0 = Color(0xFFF2EDFF)
    val WooPurple5 = Color(0xFFDFD1FB)
    val WooPurple10 = Color(0xFFCFB9F6)
    val WooPurple20 = Color(0xFFBEA0F2)
    val WooPurple30 = Color(0xFFAD86E9)
    val WooPurple40 = Color(0xFF966CCF)
    val WooPurple50 = Color(0xFF7F54B3)
    val WooPurple60 = Color(0xFF674399)
    val WooPurple70 = Color(0xFF533582)
    val WooPurple80 = Color(0xFF3C2861)
    val WooPurple90 = Color(0xFF271B3D)
    val WooPurple100 = Color(0xFF140E1F)

    val Purple10 = Color(0xFFF7EDF7)
    val Purple15 = Color(0xFFE5CFE8)
    val Purple20 = Color(0xFFC792E0)
    val Purple30 = Color(0xFFB17FD4)
    val Purple40 = Color(0xFFAF7DD1)
    val Purple50 = Color(0xFF7F54B3)
    val Purple60 = Color(0xFF674399)
    val Purple60Alpha33 = Color(0x33674399)
    val Purple80 = Color(0xFF3C2861)
    val Purple90 = Color(0xFF271B3D)

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

    val White = Color(0xFFFFFFFF)
    val WhiteAlpha005 = Color(0x0DFFFFFF)
    val WhiteAlpha008 = Color(0x14FFFFFF)
    val WhiteAlpha009 = Color(0x17FFFFFF)
    val WhiteAlpha012 = Color(0x1FFFFFFF)
    val WhiteAlpha038 = Color(0x61FFFFFF)
    val WhiteAlpha060 = Color(0x99FFFFFF)
    val WhiteAlpha087 = Color(0xDEFFFFFF)

    val Gray0 = Color(0xFFF6F7F7)
    val Gray5 = Color(0xFFDCDCDE)
    val Gray6 = Color(0xFFF2F2F7)
    val Gray20 = Color(0xFFB4B1B8)
    val Gray40 = Color(0xFF787C82)
    val Gray60 = Color(0xFF51565F)
    val Gray70 = Color(0xFF3D444B)
    val Gray80 = Color(0xFF2C3338)
    val Gray80Alpha012 = Color(0x1F2C3338)
    val Gray80Alpha030 = Color(0x4D3C3C43)
    val Gray900 = Color(0xFFF7F7F7)

    val Black = Color(0xFF000000)
    val Black90 = Color(0xFF121212)
    val Black90Alpha004 = Color(0x0A000000)
    val Black90Alpha012 = Color(0x1F121212)
    val Black90Alpha020 = Color(0x33121212)
    val Black90Alpha038 = Color(0x61121212)
    val Black90Alpha060 = Color(0x99121212)
    val Black90Alpha087 = Color(0xDE121212)
    val Black900 = Color(0xFF272727)
    val Black60 = Color(0xFF6A6A6A)
    val Black80 = Color(0xFF363636)
    val BlackAlpha008 = Color(0x14212121)
}

private val DarkColorPalette = darkColors(
    primary = WooPosColors.primary,
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
    primary = WooPosColors.Purple50,
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
    success = WooPosColors.darkCustomColorsSuccess,
    error = WooPosColors.darkCustomColorsError,
    paymentSuccessBackground = WooPosColors.darkCustomColorsPaymentSuccessBackground,
    paymentSuccessText = WooPosColors.oldGrayLight,
    paymentSuccessIcon = Color.White,
    paymentSuccessIconBackground = WooPosColors.darkCustomColorsPaymentSuccessIconBackground,
    homeBackground = WooPosColors.darkCustomColorsHomeBackground
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = WooPosColors.lightCustomColorsLoadingSkeleton,
    border = WooPosColors.lightCustomColorsBorder,
    success = WooPosColors.lightCustomColorsSuccess,
    error = WooPosColors.lightCustomColorsError,
    paymentSuccessBackground = WooPosColors.lightCustomColorsPaymentSuccessBackground,
    paymentSuccessText = WooPosColors.Purple90,
    paymentSuccessIcon = WooPosColors.lightCustomColorsSuccess,
    paymentSuccessIconBackground = Color.White,
    homeBackground = WooPosColors.Gray0
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
