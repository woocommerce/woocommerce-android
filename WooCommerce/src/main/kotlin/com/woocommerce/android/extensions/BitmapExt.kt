package com.woocommerce.android.extensions

import android.graphics.Bitmap

internal fun Bitmap.upscaleTo(desiredWidth: Int): Bitmap {
    val ratio = this.height.toFloat() / this.width.toFloat()
    val proportionateHeight = ratio * desiredWidth
    val finalHeight = Math.rint(proportionateHeight.toDouble()).toInt()

    return Bitmap.createScaledBitmap(this, desiredWidth, finalHeight, true)
}
