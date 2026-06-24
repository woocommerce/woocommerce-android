package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
@Suppress("LongParameterList")
data class WooColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: WooBackgroundColors,
    val surface: WooSurfaceColors,
    val outline: Color,
    val outlineVariant: Color,
    val status: WooStatusColors,
    val overlay: WooOverlayColors,
    val alert: WooAlertColors,
    val palette: WooPaletteColors,
)

@Immutable
data class WooBackgroundColors(
    val section: Color,
    val onSection: Color,
    val sectionVariant: Color,
    val onSectionVariant: Color,
)

@Immutable
@Suppress("LongParameterList")
data class WooSurfaceColors(
    val default: Color,
    val onDefault: Color,
    val onVariant: Color,
    val onLowest: Color,
    val onHighest: Color,
    val inverted: Color,
    val onInverted: Color,
    val onInvertedVariant: Color,
)

@Immutable
@Suppress("LongParameterList")
data class WooStatusColors(
    val errorContainer: Color,
    val onErrorContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val neutralOutlinedContainer: Color,
    val onNeutralOutlinedContainer: Color,
)

@Immutable
data class WooOverlayColors(
    val overlay20: Color,
    val overlay50: Color,
)

@Immutable
data class WooAlertColors(
    val red: Color,
    val yellow: Color,
    val green: Color,
    val blue: Color,
)

@Immutable
data class WooPaletteColors(
    val sandstone: WooSandstoneColors,
    val wooBlue: WooPaletteRampColors,
    val wooGreen: WooPaletteRampColors,
    val wooOrange: WooPaletteRampColors,
    val wooPink: WooPaletteRampColors,
    val wooPurple: WooPurpleColors,
)

@Immutable
data class WooSandstoneColors(
    val shade5: Color,
    val shade10: Color,
    val shade20: Color,
    val shade40: Color,
    val shade60: Color,
)

@Immutable
data class WooPaletteRampColors(
    val shade20: Color,
    val shade40: Color,
    val shade60: Color,
)

@Immutable
@Suppress("LongParameterList")
data class WooPurpleColors(
    val shade0: Color,
    val shade5: Color,
    val shade10: Color,
    val shade20: Color,
    val shade30: Color,
    val shade40: Color,
    val shade50: Color,
    val shade60: Color,
    val shade70: Color,
    val shade80: Color,
    val shade90: Color,
    val shade100: Color,
)

internal val LocalWooColors = staticCompositionLocalOf<WooColors> {
    error("WooTheme.colors is not available. Wrap content in WooDesignSystemTheme.")
}

@Suppress("MagicNumber")
internal val FixedWooPaletteColors = WooPaletteColors(
    sandstone = WooSandstoneColors(
        shade5 = color(0xFFFBF9F6),
        shade10 = color(0xFFF1EEEB),
        shade20 = color(0xFFE6E2DE),
        shade40 = color(0xFFC5C2BF),
        shade60 = color(0xFF8B8A89),
    ),
    wooBlue = WooPaletteRampColors(
        shade20 = color(0xFF75FFFF),
        shade40 = color(0xFF1AD0FD),
        shade60 = color(0xFF05096C),
    ),
    wooGreen = WooPaletteRampColors(
        shade20 = color(0xFFD5FF4A),
        shade40 = color(0xFF06E782),
        shade60 = color(0xFF083D2D),
    ),
    wooOrange = WooPaletteRampColors(
        shade20 = color(0xFFFFE500),
        shade40 = color(0xFFFF9000),
        shade60 = color(0xFFFF4800),
    ),
    wooPink = WooPaletteRampColors(
        shade20 = color(0xFFFCA8FF),
        shade40 = color(0xFFFF45E3),
        shade60 = color(0xFF4E0061),
    ),
    wooPurple = WooPurpleColors(
        shade0 = color(0xFFF2EDFF),
        shade5 = color(0xFFE1D7FF),
        shade10 = color(0xFFD1C1FF),
        shade20 = color(0xFFB999FF),
        shade30 = color(0xFFA77EFF),
        shade40 = color(0xFF873EFF),
        shade50 = color(0xFF720EEC),
        shade60 = color(0xFF6108CE),
        shade70 = color(0xFF5007AA),
        shade80 = color(0xFF3C087E),
        shade90 = color(0xFF2C045D),
        shade100 = color(0xFF1F0342),
    ),
)

private fun color(argb: Long): Color = Color(argb)
