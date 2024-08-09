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
    primary = WooColors.Purple50,
    primaryVariant = WooColors.Purple40,
    onPrimary = WooColors.White,
    secondary = WooColors.Green10,
    secondaryVariant = WooColors.Gray40,
    surface = WooColors.Gray80,
    onSurface = WooColors.White,
    background = WooColors.Gray80,
    onBackground = WooColors.White,
)

private val LightColorPalette = lightColors(
    primary = WooColors.Purple50,
    primaryVariant = WooColors.Purple40,
    onPrimary = WooColors.White,
    secondary = WooColors.Green10,
    secondaryVariant = WooColors.Gray40,
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
    wooPink10 = WooColors.Pink90,
    wooPink30 = WooColors.Pink70,
    wooPink50 = WooColors.Pink50,
    wooPink70 = WooColors.Pink30,
    wooPink90 = WooColors.Pink10,
    wooRed5 = WooColors.Red70,
    wooRed10 = WooColors.Red60,
    wooRed20 = WooColors.Red50,
    wooRed30 = WooColors.Red30,
    wooRed50 = WooColors.Red20,
    wooRed60 = WooColors.Red10,
    wooRed70 = WooColors.Red5,
    wooBlue5 = WooColors.Blue30,
    wooBlue30 = WooColors.Blue30,
    wooBlue40 = WooColors.Blue40,
    wooBlue50 = WooColors.Blue50,
    wooOrange5 = WooColors.Orange50,
    wooOrange10 = WooColors.Orange30,
    wooOrange30 = WooColors.Orange30,
    wooOrange50 = WooColors.Orange10,
    wooOrange70 = WooColors.Orange5,
    wooYellow10 = WooColors.Yellow50,
    wooYellow20 = WooColors.Yellow30,
    wooYellow30 = WooColors.Yellow30,
    wooYellow50 = WooColors.Yellow20,
    wooYellow70 = WooColors.Yellow10,
    wooCeladon0 = WooColors.Celadon40,
    wooCeladon5 = WooColors.Celadon5,
    wooCeladon10 = WooColors.Celadon5,
    wooCeladon20 = WooColors.Celadon10,
    wooCeladon40 = WooColors.Celadon20,
    wooGreen0 = WooColors.Green50,
    wooGreen5 = WooColors.Green5,
    wooGreen10 = WooColors.Green5,
    wooGreen20 = WooColors.Green10,
    wooGreen50 = WooColors.Green20,
    wooWhite = WooColors.White,
    wooGray0 = WooColors.Gray80,
    wooGray5 = WooColors.Gray5,
    wooGray6 = WooColors.Gray6,
    wooGray20 = WooColors.Gray40,
    wooGray40 = WooColors.Gray40,
    wooGray60 = WooColors.Gray20,
    wooGray70 = WooColors.Gray60,
    wooGray80 = WooColors.Gray60,
    wooBlack = WooColors.Black
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
    wooPink10 = WooColors.Pink90,
    wooPink30 = WooColors.Pink70,
    wooPink50 = WooColors.Pink50,
    wooPink70 = WooColors.Pink30,
    wooPink90 = WooColors.Pink10,
    wooRed5 = WooColors.Red70,
    wooRed10 = WooColors.Red60,
    wooRed20 = WooColors.Red50,
    wooRed30 = WooColors.Red30,
    wooRed50 = WooColors.Red20,
    wooRed60 = WooColors.Red10,
    wooRed70 = WooColors.Red5,
    wooBlue5 = WooColors.Blue30,
    wooBlue30 = WooColors.Blue30,
    wooBlue40 = WooColors.Blue40,
    wooBlue50 = WooColors.Blue50,
    wooOrange5 = WooColors.Orange50,
    wooOrange10 = WooColors.Orange30,
    wooOrange30 = WooColors.Orange30,
    wooOrange50 = WooColors.Orange10,
    wooOrange70 = WooColors.Orange5,
    wooYellow10 = WooColors.Yellow50,
    wooYellow20 = WooColors.Yellow30,
    wooYellow30 = WooColors.Yellow30,
    wooYellow50 = WooColors.Yellow20,
    wooYellow70 = WooColors.Yellow10,
    wooCeladon0 = WooColors.Celadon40,
    wooCeladon5 = WooColors.Celadon5,
    wooCeladon10 = WooColors.Celadon5,
    wooCeladon20 = WooColors.Celadon10,
    wooCeladon40 = WooColors.Celadon20,
    wooGreen0 = WooColors.Green50,
    wooGreen5 = WooColors.Green5,
    wooGreen10 = WooColors.Green5,
    wooGreen20 = WooColors.Green10,
    wooGreen50 = WooColors.Green20,
    wooWhite = WooColors.White,
    wooGray0 = WooColors.Gray80,
    wooGray5 = WooColors.Gray5,
    wooGray6 = WooColors.Gray6,
    wooGray20 = WooColors.Gray40,
    wooGray40 = WooColors.Gray40,
    wooGray60 = WooColors.Gray20,
    wooGray70 = WooColors.Gray60,
    wooGray80 = WooColors.Gray60,
    wooBlack = WooColors.Black
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
