package com.woocommerce.android.ui.compose.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.woocommerce.android.R

val LightColors
    @Composable
    get() = lightColors(
        primary = colorResource(R.color.color_primary),
        primaryVariant = colorResource(R.color.color_primary_variant),
        secondary = colorResource(R.color.color_secondary),
        secondaryVariant = colorResource(R.color.color_secondary_variant),
        background = colorResource(R.color.default_window_background),
        surface = colorResource(R.color.color_surface),
        error = colorResource(R.color.color_error),
        onPrimary = colorResource(R.color.color_on_primary),
        onSecondary = colorResource(R.color.color_on_secondary),
        onBackground = colorResource(R.color.color_on_background),
        onSurface = colorResource(R.color.color_on_surface),
        onError = colorResource(R.color.color_on_error),
    )

val DarkColors
    @Composable
    get() = darkColors(
        primary = colorResource(R.color.color_primary),
        primaryVariant = colorResource(R.color.color_primary_variant),
        secondary = colorResource(R.color.color_secondary),
        secondaryVariant = colorResource(R.color.color_secondary_variant),
        background = colorResource(R.color.default_window_background),
        surface = colorResource(R.color.color_surface),
        error = colorResource(R.color.color_error),
        onPrimary = colorResource(R.color.color_on_primary),
        onSecondary = colorResource(R.color.color_on_secondary),
        onBackground = colorResource(R.color.color_on_background),
        onSurface = colorResource(R.color.color_on_surface),
        onError = colorResource(R.color.color_on_error),
    )
