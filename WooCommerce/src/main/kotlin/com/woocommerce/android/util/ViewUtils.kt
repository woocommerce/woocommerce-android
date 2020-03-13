package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.R

/**
 * Converts a pixel to a density pixel to match the density of
 * the device.
 */
fun getDensityPixel(context: Context, dps: Int): Int {
    val scale = context.resources.displayMetrics.density
    return (dps * scale + 0.5f).toInt()
}

fun isTabletMode(context: Context) = context.resources.getBoolean(R.bool.is_tablet)

fun isLandscapeMode(context: Context) = context.resources.getBoolean(R.bool.is_landscape)
