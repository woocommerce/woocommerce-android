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

/**
 * Checks if the specs of this device would support the split screen tablet mode.
 *
 * @return True if split screen supported, else false.
 */
fun isSplitScreenSupported(context: Context) = context.resources.getBoolean(R.bool.split_screen_supported)
