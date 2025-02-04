package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import android.util.TypedValue
import androidx.core.view.WindowInsetsCompat

// That seems to be different on different devices, but 32dp is a common upper value
private const val GESTURE_NAVIGATION_BAR_HEIGHT_DP = 32
fun WindowInsetsCompat.isGestureNavigation(context: Context): Boolean {
    val bottomInset = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    val gestureNavigationBarHeightPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        GESTURE_NAVIGATION_BAR_HEIGHT_DP.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    return bottomInset in 1..gestureNavigationBarHeightPx
}
