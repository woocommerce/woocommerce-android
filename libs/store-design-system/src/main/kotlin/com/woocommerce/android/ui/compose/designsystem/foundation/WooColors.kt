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

internal fun wooColors(useDarkTheme: Boolean): WooColors =
    if (useDarkTheme) {
        DarkWooColors
    } else {
        LightWooColors
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

private val LightWooColors = WooColors(
    primary = FixedWooPaletteColors.wooPurple.shade40,
    onPrimary = color(0xFFFFFFFF),
    secondary = color(0xFFEAE2FE),
    onSecondary = FixedWooPaletteColors.wooPurple.shade40,
    background = WooBackgroundColors(
        section = color(0xFFF2F2F8),
        onSection = color(0xFF1E1E1E),
        sectionVariant = color(0xFFF0F0F0),
        onSectionVariant = color(0xFF1C1C1E),
    ),
    surface = WooSurfaceColors(
        default = color(0xFFFFFFFF),
        onDefault = color(0xFF1E1E1E),
        onVariant = color(0xFF868A94),
        onLowest = color(0xFFB2B7C0),
        onHighest = color(0xFF50575E),
        inverted = color(0xFF1C1C1E),
        onInverted = color(0xFFFFFFFF),
        onInvertedVariant = color(0xFF929298),
    ),
    outline = color(0xFF787C82),
    outlineVariant = color(0xFFD2D2D8),
    status = WooStatusColors(
        errorContainer = color(0xFFF6E6E3),
        onErrorContainer = color(0xFF470000),
        warningContainer = color(0xFFFDE6BE),
        onWarningContainer = color(0xFF2E1900),
        cautionContainer = color(0xFFFEE995),
        onCautionContainer = color(0xFF281D00),
        successContainer = color(0xFFC6F7CD),
        onSuccessContainer = color(0xFF002900),
        infoContainer = color(0xFFDEEBFA),
        onInfoContainer = color(0xFF001B4F),
        neutralContainer = color(0xFFF4F4F4),
        onNeutralContainer = color(0xFF1E1E1E),
        neutralOutlinedContainer = color(0xFFDBDBDB),
        onNeutralOutlinedContainer = color(0xFF1E1E1E),
    ),
    overlay = WooOverlayColors(
        overlay20 = color(0xFF000000, alpha = 0.2f),
        overlay50 = color(0xFF000000, alpha = 0.5f),
    ),
    alert = WooAlertColors(
        red = color(0xFFFC4A5B),
        yellow = color(0xFFEAAB2D),
        green = color(0xFF27AE32),
        blue = color(0xFF1E94D0),
    ),
    palette = FixedWooPaletteColors,
)

private val DarkWooColors = WooColors(
    primary = FixedWooPaletteColors.wooPurple.shade40,
    onPrimary = color(0xFFFFFFFF),
    secondary = color(0xFF383146),
    onSecondary = color(0xFFF1EDFE),
    background = WooBackgroundColors(
        section = color(0xFF101517),
        onSection = color(0xFFFFFFFF),
        sectionVariant = color(0xFF101517),
        onSectionVariant = color(0xFF8B8A8E),
    ),
    surface = WooSurfaceColors(
        default = color(0xFF232529),
        onDefault = color(0xFFFFFFFF),
        onVariant = color(0xFF868A94),
        onLowest = color(0xFF626068),
        onHighest = color(0xFF626068),
        inverted = color(0xFFFFFFFF),
        onInverted = color(0xFF1E1E1E),
        onInvertedVariant = color(0xFF8D8D91),
    ),
    outline = color(0xFF454549),
    outlineVariant = color(0xFF5E5E63),
    status = WooStatusColors(
        errorContainer = color(0xFFF6E6E3, alpha = 0.9f),
        onErrorContainer = color(0xFF470000),
        warningContainer = color(0xFFFDE6BE, alpha = 0.9f),
        onWarningContainer = color(0xFF2E1900),
        cautionContainer = color(0xFFFEE995, alpha = 0.9f),
        onCautionContainer = color(0xFF281D00),
        successContainer = color(0xFFC6F7CD, alpha = 0.9f),
        onSuccessContainer = color(0xFF002900),
        infoContainer = color(0xFFDEEBFA, alpha = 0.9f),
        onInfoContainer = color(0xFF001B4F),
        neutralContainer = color(0xFFF4F4F4, alpha = 0.9f),
        onNeutralContainer = color(0xFF1E1E1E),
        neutralOutlinedContainer = color(0xFFDBDBDB, alpha = 0.9f),
        onNeutralOutlinedContainer = color(0xFFDBDBDB),
    ),
    overlay = WooOverlayColors(
        overlay20 = color(0xFF000000, alpha = 0.2f),
        overlay50 = color(0xFF000000, alpha = 0.75f),
    ),
    alert = WooAlertColors(
        red = color(0xFFDC3545),
        yellow = color(0xFFEAAB2D),
        green = color(0xFF69B66F),
        blue = color(0xFF1E94D0),
    ),
    palette = FixedWooPaletteColors,
)

private fun color(argb: Long): Color = Color(argb)

private fun color(argb: Long, alpha: Float): Color = Color(argb).copy(alpha = alpha)
