package com.woocommerce.android.ui.compose.designsystem.foundation

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.ui.compose.designsystem.R

@Composable
internal fun wooColors(useDarkTheme: Boolean): WooColors {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    return remember(context, configuration, useDarkTheme) {
        context.createWooColorContext(useDarkTheme = useDarkTheme).loadWooColors()
    }
}

internal fun loadWooPaletteColors(context: Context): WooPaletteColors = WooPaletteColors(
    sandstone = WooSandstoneColors(
        shade5 = context.color(R.color.woo_ds_color_palette_sandstone_shade5),
        shade10 = context.color(R.color.woo_ds_color_palette_sandstone_shade10),
        shade20 = context.color(R.color.woo_ds_color_palette_sandstone_shade20),
        shade40 = context.color(R.color.woo_ds_color_palette_sandstone_shade40),
        shade60 = context.color(R.color.woo_ds_color_palette_sandstone_shade60),
    ),
    gray = WooGrayColors(
        shade0 = context.color(R.color.woo_ds_color_palette_gray_shade0),
        shade5 = context.color(R.color.woo_ds_color_palette_gray_shade5),
        shade10 = context.color(R.color.woo_ds_color_palette_gray_shade10),
        shade20 = context.color(R.color.woo_ds_color_palette_gray_shade20),
        shade30 = context.color(R.color.woo_ds_color_palette_gray_shade30),
        shade40 = context.color(R.color.woo_ds_color_palette_gray_shade40),
        shade50 = context.color(R.color.woo_ds_color_palette_gray_shade50),
        shade60 = context.color(R.color.woo_ds_color_palette_gray_shade60),
        shade70 = context.color(R.color.woo_ds_color_palette_gray_shade70),
        shade80 = context.color(R.color.woo_ds_color_palette_gray_shade80),
        shade90 = context.color(R.color.woo_ds_color_palette_gray_shade90),
        shade100 = context.color(R.color.woo_ds_color_palette_gray_shade100),
    ),
    wooBlue = WooPaletteRampColors(
        shade20 = context.color(R.color.woo_ds_color_palette_woo_blue_shade20),
        shade40 = context.color(R.color.woo_ds_color_palette_woo_blue_shade40),
        shade60 = context.color(R.color.woo_ds_color_palette_woo_blue_shade60),
    ),
    wooGreen = WooPaletteRampColors(
        shade20 = context.color(R.color.woo_ds_color_palette_woo_green_shade20),
        shade40 = context.color(R.color.woo_ds_color_palette_woo_green_shade40),
        shade60 = context.color(R.color.woo_ds_color_palette_woo_green_shade60),
    ),
    wooOrange = WooPaletteRampColors(
        shade20 = context.color(R.color.woo_ds_color_palette_woo_orange_shade20),
        shade40 = context.color(R.color.woo_ds_color_palette_woo_orange_shade40),
        shade60 = context.color(R.color.woo_ds_color_palette_woo_orange_shade60),
    ),
    wooPink = WooPaletteRampColors(
        shade20 = context.color(R.color.woo_ds_color_palette_woo_pink_shade20),
        shade40 = context.color(R.color.woo_ds_color_palette_woo_pink_shade40),
        shade60 = context.color(R.color.woo_ds_color_palette_woo_pink_shade60),
    ),
    wooPurple = WooPurpleColors(
        shade0 = context.color(R.color.woo_ds_color_palette_woo_purple_shade0),
        shade5 = context.color(R.color.woo_ds_color_palette_woo_purple_shade5),
        shade10 = context.color(R.color.woo_ds_color_palette_woo_purple_shade10),
        shade20 = context.color(R.color.woo_ds_color_palette_woo_purple_shade20),
        shade30 = context.color(R.color.woo_ds_color_palette_woo_purple_shade30),
        shade40 = context.color(R.color.woo_ds_color_palette_woo_purple_shade40),
        shade50 = context.color(R.color.woo_ds_color_palette_woo_purple_shade50),
        shade60 = context.color(R.color.woo_ds_color_palette_woo_purple_shade60),
        shade70 = context.color(R.color.woo_ds_color_palette_woo_purple_shade70),
        shade80 = context.color(R.color.woo_ds_color_palette_woo_purple_shade80),
        shade90 = context.color(R.color.woo_ds_color_palette_woo_purple_shade90),
        shade100 = context.color(R.color.woo_ds_color_palette_woo_purple_shade100),
    ),
)

@Suppress("LongMethod")
private fun Context.loadWooColors(): WooColors {
    val palette = loadWooPaletteColors(this)

    return WooColors(
        primary = color(R.color.woo_ds_color_primary),
        onPrimary = color(R.color.woo_ds_color_on_primary),
        secondary = color(R.color.woo_ds_color_secondary),
        onSecondary = color(R.color.woo_ds_color_on_secondary),
        container = WooContainerColors(
            primaryContainer = color(R.color.woo_ds_color_container_primary_container),
            onPrimaryContainer = color(R.color.woo_ds_color_container_on_primary_container),
            secondaryContainer = color(R.color.woo_ds_color_container_secondary_container),
            onSecondaryContainer = color(R.color.woo_ds_color_container_on_secondary_container),
        ),
        background = WooBackgroundColors(
            section = color(R.color.woo_ds_color_background_section),
            onSection = color(R.color.woo_ds_color_background_on_section),
            sectionVariant = color(R.color.woo_ds_color_background_section_variant),
            onSectionVariant = color(R.color.woo_ds_color_background_on_section_variant),
        ),
        surface = WooSurfaceColors(
            default = color(R.color.woo_ds_color_surface_default),
            surfaceDim = color(R.color.woo_ds_color_surface_dim),
            surfaceContainerHighest = color(R.color.woo_ds_color_surface_container_highest),
            onDefault = color(R.color.woo_ds_color_surface_on_default),
            onVariant = color(R.color.woo_ds_color_surface_on_variant),
            onVariantLowest = color(R.color.woo_ds_color_surface_on_variant_lowest),
            inverted = color(R.color.woo_ds_color_surface_inverted),
            onInverted = color(R.color.woo_ds_color_surface_on_inverted),
        ),
        outline = color(R.color.woo_ds_color_outline),
        outlineVariant = color(R.color.woo_ds_color_outline_variant),
        status = WooStatusColors(
            errorContainer = color(R.color.woo_ds_color_status_error_container),
            onErrorContainer = color(R.color.woo_ds_color_status_on_error_container),
            warningContainer = color(R.color.woo_ds_color_status_warning_container),
            onWarningContainer = color(R.color.woo_ds_color_status_on_warning_container),
            cautionContainer = color(R.color.woo_ds_color_status_caution_container),
            onCautionContainer = color(R.color.woo_ds_color_status_on_caution_container),
            successContainer = color(R.color.woo_ds_color_status_success_container),
            onSuccessContainer = color(R.color.woo_ds_color_status_on_success_container),
            infoContainer = color(R.color.woo_ds_color_status_info_container),
            onInfoContainer = color(R.color.woo_ds_color_status_on_info_container),
            neutralContainer = color(R.color.woo_ds_color_status_neutral_container),
            onNeutralContainer = color(R.color.woo_ds_color_status_on_neutral_container),
        ),
        overlay = WooOverlayColors(
            overlay20 = color(R.color.woo_ds_color_overlay_overlay20),
            overlay50 = color(R.color.woo_ds_color_overlay_overlay50),
        ),
        alert = WooAlertColors(
            red = color(R.color.woo_ds_color_alert_red),
            onRed = color(R.color.woo_ds_color_alert_on_red),
            orange = color(R.color.woo_ds_color_alert_orange),
            onOrange = color(R.color.woo_ds_color_alert_on_orange),
            green = color(R.color.woo_ds_color_alert_green),
            onGreen = color(R.color.woo_ds_color_alert_on_green),
            blue = color(R.color.woo_ds_color_alert_blue),
            onBlue = color(R.color.woo_ds_color_alert_on_blue),
        ),
        palette = palette,
    )
}

private fun Context.createWooColorContext(useDarkTheme: Boolean): Context {
    val colorConfiguration = Configuration(resources.configuration).apply {
        uiMode = uiMode.withNightMode(
            nightMode = if (useDarkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO,
        )
    }

    return createConfigurationContext(colorConfiguration)
}

private fun Int.withNightMode(nightMode: Int): Int {
    val clearNightMask = Configuration.UI_MODE_NIGHT_MASK.inv()
    return (this and clearNightMask) or nightMode
}

private fun Context.color(@ColorRes colorRes: Int): Color = Color(resources.getColor(colorRes, theme))
