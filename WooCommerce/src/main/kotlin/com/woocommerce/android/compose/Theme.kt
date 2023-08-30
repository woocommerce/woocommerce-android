package com.woocommerce.android.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.woocommerce.android.compose.WooColors.md_theme_dark_background
import com.woocommerce.android.compose.WooColors.md_theme_dark_error
import com.woocommerce.android.compose.WooColors.md_theme_dark_onBackground
import com.woocommerce.android.compose.WooColors.md_theme_dark_onError
import com.woocommerce.android.compose.WooColors.md_theme_dark_onPrimary
import com.woocommerce.android.compose.WooColors.md_theme_dark_onSecondary
import com.woocommerce.android.compose.WooColors.md_theme_dark_onSurface
import com.woocommerce.android.compose.WooColors.md_theme_dark_primary
import com.woocommerce.android.compose.WooColors.md_theme_dark_primary_variant
import com.woocommerce.android.compose.WooColors.md_theme_dark_secondary
import com.woocommerce.android.compose.WooColors.md_theme_dark_secondary_variant
import com.woocommerce.android.compose.WooColors.md_theme_dark_surface
import com.woocommerce.android.compose.WooColors.md_theme_light_background
import com.woocommerce.android.compose.WooColors.md_theme_light_error
import com.woocommerce.android.compose.WooColors.md_theme_light_onBackground
import com.woocommerce.android.compose.WooColors.md_theme_light_onError
import com.woocommerce.android.compose.WooColors.md_theme_light_onPrimary
import com.woocommerce.android.compose.WooColors.md_theme_light_onSecondary
import com.woocommerce.android.compose.WooColors.md_theme_light_onSurface
import com.woocommerce.android.compose.WooColors.md_theme_light_primary
import com.woocommerce.android.compose.WooColors.md_theme_light_primary_variant
import com.woocommerce.android.compose.WooColors.md_theme_light_secondary
import com.woocommerce.android.compose.WooColors.md_theme_light_secondary_variant
import com.woocommerce.android.compose.WooColors.md_theme_light_surface

private val LightColors = lightColors(
    primary = md_theme_light_primary,
    primaryVariant = md_theme_light_primary_variant,
    secondary = md_theme_light_secondary,
    secondaryVariant = md_theme_light_secondary_variant,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    error = md_theme_light_error,
    onPrimary = md_theme_light_onPrimary,
    onSecondary = md_theme_light_onSecondary,
    onBackground = md_theme_light_onBackground,
    onSurface = md_theme_light_onSurface,
    onError = md_theme_light_onError,
)

private val DarkColors = darkColors(
    primary = md_theme_dark_primary,
    primaryVariant = md_theme_dark_primary_variant,
    secondary = md_theme_dark_secondary,
    secondaryVariant = md_theme_dark_secondary_variant,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    error = md_theme_dark_error,
    onPrimary = md_theme_dark_onPrimary,
    onSecondary = md_theme_dark_onSecondary,
    onBackground = md_theme_dark_onBackground,
    onSurface = md_theme_dark_onSurface,
    onError = md_theme_dark_onError,
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}
