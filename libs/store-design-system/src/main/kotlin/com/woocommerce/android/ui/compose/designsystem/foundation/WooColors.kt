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
    val container: WooContainerColors,
    val background: WooBackgroundColors,
    val surface: WooSurfaceColors,
    val outline: Color,
    val outlineVariant: Color,
    val status: WooStatusColors,
    val overlay: WooOverlayColors,
    val alert: WooAlertColors,
    val palette: WooPaletteColors,
    val stateLayers: WooStateLayerColors,
    val error: Color,
    val onError: Color,
    val tintLayers: WooTintLayerColors,
)

@Immutable
data class WooContainerColors(
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
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
    val surfaceDim: Color,
    val surfaceContainerHighest: Color,
    val onDefault: Color,
    val onVariant: Color,
    val onVariantLowest: Color,
    val inverted: Color,
    val onInverted: Color,
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
)

@Immutable
data class WooOverlayColors(
    val overlay20: Color,
    val overlay50: Color,
)

@Immutable
data class WooStateLayerColors(
    val onSurface: WooOpacityColors,
)

@Immutable
data class WooTintLayerColors(
    val primaryContainer: WooOpacityColors,
)

@Immutable
data class WooOpacityColors(
    val opacity08: Color,
    val opacity10: Color,
    val opacity16: Color,
    val opacity24: Color,
)

@Immutable
data class WooAlertColors(
    val red: Color,
    val onRed: Color,
    val orange: Color,
    val onOrange: Color,
    val green: Color,
    val onGreen: Color,
    val blue: Color,
    val onBlue: Color,
)

@Immutable
data class WooPaletteColors(
    val sandstone: WooSandstoneColors,
    val gray: WooGrayColors,
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
@Suppress("LongParameterList")
data class WooGrayColors(
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
