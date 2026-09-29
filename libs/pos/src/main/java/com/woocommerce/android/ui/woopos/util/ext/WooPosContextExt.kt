package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.woocommerce.android.ui.woopos.util.WooPosScreenSizeUtils
import kotlin.math.max
import kotlin.math.min

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "woo_pos_data_store")

data class WooPosPhysicalScreenSizeDp(val shortSide: Int, val longSide: Int)

fun Context.isWooPosPhoneLayout(): Boolean {
    val size = getPhysicalScreenSizeDp()
    return !WooPosScreenSizeUtils.isTabletSize(size.shortSide.dp, size.longSide.dp)
}

fun Context.getPhysicalScreenSizeDp(): WooPosPhysicalScreenSizeDp {
    val display = checkNotNull(getSystemService(DisplayManager::class.java)).getDisplay(Display.DEFAULT_DISPLAY)
    val density = resources.displayMetrics.density
    val (widthPx, heightPx) = getPhysicalDisplaySizePx(checkNotNull(display))
    val widthDp = (widthPx / density).toInt()
    val heightDp = (heightPx / density).toInt()
    return WooPosPhysicalScreenSizeDp(shortSide = min(widthDp, heightDp), longSide = max(widthDp, heightDp))
}

private fun Context.getPhysicalDisplaySizePx(display: Display): Pair<Int, Int> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = checkNotNull(createDisplayContext(display).getSystemService(WindowManager::class.java))
            .maximumWindowMetrics
            .bounds
        (bounds.right - bounds.left) to (bounds.bottom - bounds.top)
    } else {
        val size = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(size)
        size.x to size.y
    }

fun Context.getScreenWidthDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}

fun Context.getScreenHeightDp(): Int {
    val displayMetrics = resources.displayMetrics
    return (displayMetrics.heightPixels / displayMetrics.density).toInt()
}
