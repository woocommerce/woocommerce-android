package com.woocommerce.android.util

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.ScrollView

/**
 * Converts a pixel to a density pixel to match the density of
 * the device.
 */
fun getDensityPixel(context: Context, dps: Int): Int {
    val scale = context.resources.displayMetrics.density
    return (dps * scale + 0.5f).toInt()
}

fun isViewVisibleInScrollView(scrollView: ScrollView, targetView: View): Boolean {
    val scrollBounds = Rect()
    scrollView.getDrawingRect(scrollBounds)
    val top = targetView.y + scrollView.y
    val bottom = top + targetView.height
    return scrollBounds.top < top && scrollBounds.bottom > bottom
}
