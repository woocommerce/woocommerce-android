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
    val wooPink10: Color,
    val wooPink30: Color,
    val wooPink50: Color,
    val wooPink70: Color,
    val wooPink90: Color,
    val wooRed5: Color,
    val wooRed10: Color,
    val wooRed20: Color,
    val wooRed30: Color,
    val wooRed50: Color,
    val wooRed60: Color,
    val wooRed70: Color,
    val wooBlue5: Color,
    val wooBlue30: Color,
    val wooBlue40: Color,
    val wooBlue50: Color,
    val wooOrange5: Color,
    val wooOrange10: Color,
    val wooOrange30: Color,
    val wooOrange50: Color,
    val wooOrange70: Color,
    val wooYellow10: Color,
    val wooYellow20: Color,
    val wooYellow30: Color,
    val wooYellow50: Color,
    val wooYellow70: Color,
    val wooCeladon0: Color,
    val wooCeladon5: Color,
    val wooCeladon10: Color,
    val wooCeladon20: Color,
    val wooCeladon40: Color,
    val wooGreen0: Color,
    val wooGreen5: Color,
    val wooGreen10: Color,
    val wooGreen20: Color,
    val wooGreen50: Color,
    val wooWhite: Color,
    val wooGray0: Color,
    val wooGray5: Color,
    val wooGray6: Color,
    val wooGray20: Color,
    val wooGray40: Color,
    val wooGray60: Color,
    val wooGray70: Color,
    val wooGray80: Color,
    val wooBlack: Color
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
    background = Color(0xFFF6F7F7),
    onBackground = Color.Black,
)

private val DarkCustomColors = CustomColors(
    loadingSkeleton = Color(0xFF616161),
    border = Color(0xFF8D8D8D),
    success = Color(0xFF06B166),
    error = Color(0xFFBE4400),
    paymentSuccessBackground = Color(0xFF74C758),
    paymentSuccessText = Color(0xFFF2EBFF),
    wooPurple0 = Color(0xFF271B3D),
    wooPurple10 = Color(0xFF3C2861),
    wooPurple15 = Color(0xFF674399),
    wooPurple20 = Color(0xFF7F54B3),
    wooPurple30 = Color(0xFFAF7DD1),
    wooPurple40 = Color(0xFFB17FD4),
    wooPurple50 = Color(0xFFC792E0),
    wooPurple60 = Color(0xFFE5CFE8),
    wooPurple80 = Color(0xFFF7EDF7),
    wooPurple90 = Color(0xFFF2EDFF),
    wooPink10 = Color(0xFF5C0935),
    wooPink30 = Color(0xFF880E4F),
    wooPink50 = Color(0xFFC9356E),
    wooPink70 = Color(0xFFEB6594),
    wooPink90 = Color(0xFFED9BB8),
    wooRed5 = Color(0xFF8A2424),
    wooRed10 = Color(0xFFB32D2E),
    wooRed20 = Color(0xFFD63638),
    wooRed30 = Color(0xFFF86368),
    wooRed50 = Color(0xFFFE8085),
    wooRed60 = Color(0xFFFFA6AB),
    wooRed70 = Color(0xFFFACFD2),
    wooBlue5 = Color(0xFF5198D9),
    wooBlue30 = Color(0xFF5198D9),
    wooBlue40 = Color(0xFF1689DB),
    wooBlue50 = Color(0xFF2271B1),
    wooOrange5 = Color(0xFFB26200),
    wooOrange10 = Color(0xFFE68B28),
    wooOrange30 = Color(0xFFE68B28),
    wooOrange50 = Color(0xFFFFBF86),
    wooOrange70 = Color(0xFFF7DCC6),
    wooYellow10 = Color(0xFF907300),
    wooYellow20 = Color(0xFFDBAE17),
    wooYellow30 = Color(0xFFDBAE17),
    wooYellow50 = Color(0xFFF0C443),
    wooYellow70 = Color(0xFFF2CF75),
    wooCeladon0 = Color(0xFF009172),
    wooCeladon5 = Color(0xFFA7E8D4),
    wooCeladon10 = Color(0xFFA7E8D4),
    wooCeladon20 = Color(0xFF65D9B9),
    wooCeladon40 = Color(0xFF2FC39E),
    wooGreen0 = Color(0xFF008A20),
    wooGreen5 = Color(0xFFA4F5C8),
    wooGreen10 = Color(0xFFA4F5C8),
    wooGreen20 = Color(0xFF59E38F),
    wooGreen50 = Color(0xFF1ED15A),
    wooWhite = Color(0xFFFFFFFF),
    wooGray0 = Color(0xFF2C3338),
    wooGray5 = Color(0xFFDCDCDE),
    wooGray6 = Color(0xFFF2F2F7),
    wooGray20 = Color(0xFF787C82),
    wooGray40 = Color(0xFF787C82),
    wooGray60 = Color(0xFFB4B1B8),
    wooGray70 = Color(0xFF51565F),
    wooGray80 = Color(0xFF51565F),
    wooBlack = Color(0xFF121212)
)

private val LightCustomColors = CustomColors(
    loadingSkeleton = Color(0xFFE1E1E1),
    border = Color(0xFFC6C6C8),
    success = Color(0xFF03D479),
    error = Color(0xFFF16618),
    paymentSuccessBackground = Color(0xFF98F179),
    paymentSuccessText = Color(0xFF271B3D),
    wooPurple0 = Color(0xFFF2EDFF),
    wooPurple10 = Color(0xFFF7EDF7),
    wooPurple15 = Color(0xFFE5CFE8),
    wooPurple20 = Color(0xFFC792E0),
    wooPurple30 = Color(0xFFB17FD4),
    wooPurple40 = Color(0xFFAF7DD1),
    wooPurple50 = Color(0xFF7F54B3),
    wooPurple60 = Color(0xFF674399),
    wooPurple80 = Color(0xFF3C2861),
    wooPurple90 = Color(0xFF271B3D),
    wooPink10 = Color(0xFFED9BB8),
    wooPink30 = Color(0xFFEB6594),
    wooPink50 = Color(0xFFC9356E),
    wooPink70 = Color(0xFF880E4F),
    wooPink90 = Color(0xFF5C0935),
    wooRed5 = Color(0xFFFACFD2),
    wooRed10 = Color(0xFFFFA6AB),
    wooRed20 = Color(0xFFFF8085),
    wooRed30 = Color(0xFFF86368),
    wooRed50 = Color(0xFFD63638),
    wooRed60 = Color(0xFFB32D2E),
    wooRed70 = Color(0xFF8A2424),
    wooBlue5 = Color(0xFFBBE0FA),
    wooBlue30 = Color(0xFF5198D9),
    wooBlue40 = Color(0xFF1689DB),
    wooBlue50 = Color(0xFF2271B1),
    wooOrange5 = Color(0xFFF7DCC6),
    wooOrange10 = Color(0xFFFFBF86),
    wooOrange30 = Color(0xFFE68B28),
    wooOrange50 = Color(0xFFB26200),
    wooOrange70 = Color(0xFF351F04),
    wooYellow10 = Color(0xFFF2CF75),
    wooYellow20 = Color(0xFFF0C443),
    wooYellow30 = Color(0xFFDBAE17),
    wooYellow50 = Color(0xFF907300),
    wooYellow70 = Color(0xFF5C4B00),
    wooCeladon0 = Color(0xFFECF7F4),
    wooCeladon5 = Color(0xFFA7E8D4),
    wooCeladon10 = Color(0xFF65D9B9),
    wooCeladon20 = Color(0xFF2FC39E),
    wooCeladon40 = Color(0xFF009172),
    wooGreen0 = Color(0xFFEBF7F1),
    wooGreen5 = Color(0xFFA4F5C8),
    wooGreen10 = Color(0xFF59E38F),
    wooGreen20 = Color(0xFF1ED15A),
    wooGreen50 = Color(0xFF008A20),
    wooWhite = Color(0xFFFFFFFF),
    wooGray0 = Color(0xFFF6F7F7),
    wooGray5 = Color(0xFFDCDCDE),
    wooGray6 = Color(0xFFF2F2F7),
    wooGray20 = Color(0xFFB4B1B8),
    wooGray40 = Color(0xFF787C82),
    wooGray60 = Color(0xFF51565F),
    wooGray70 = Color(0xFF3D444B),
    wooGray80 = Color(0xFF2C3338),
    wooBlack = Color(0xFF000000)
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
