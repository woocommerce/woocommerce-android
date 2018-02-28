package com.woocommerce.android.util

import android.content.Context

/**
 * Converts a pixel to a density pixel to match the density of
 * the device.
 */
fun getDensityPixel(context: Context, dps: Int): Int {
    val scale = context.resources.displayMetrics.density
    return (dps * scale + 0.5f).toInt()
}
