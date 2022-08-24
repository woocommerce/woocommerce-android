package com.woocommerce.android.extensions

import android.content.Intent
import com.woocommerce.android.ui.appwidgets.WidgetColorMode

private const val COLOR_MODE_KEY = "color_mode_key"

fun Intent.getColorMode(): WidgetColorMode {
    return this.getEnumExtra(COLOR_MODE_KEY, WidgetColorMode.LIGHT)
}

fun Intent.putColorMode(color: WidgetColorMode) {
    this.putEnumExtra(COLOR_MODE_KEY, color)
}

private inline fun <reified T : Enum<T>> Intent.putEnumExtra(key: String, victim: T): Intent =
    putExtra(key, victim.ordinal)

private inline fun <reified T : Enum<T>> Intent.getEnumExtra(key: String, default: T): T =
    getIntExtra(key, -1)
        .takeUnless { it == -1 }
        ?.let { T::class.java.enumConstants?.get(it) } ?: default
