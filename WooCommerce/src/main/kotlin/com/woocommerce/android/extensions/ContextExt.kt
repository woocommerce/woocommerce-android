package com.woocommerce.android.extensions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.woocommerce.android.util.SystemVersionUtils

val Context.windowSizeClass: WindowSizeClass
    get() = when (resources.configuration.screenWidthDp) {
        in 0 until WindowSizeClass.Compact.maxWidth -> WindowSizeClass.Compact
        in WindowSizeClass.Compact.maxWidth until WindowSizeClass.Medium.maxWidth -> WindowSizeClass.Medium
        else -> WindowSizeClass.ExpandedAndBigger
    }

/**
 * Window size class type based on Material Design
 * [guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes)
 */
sealed class WindowSizeClass(val maxWidth: Int) {
    /**
     * Phone in portrait
     */
    data object Compact : WindowSizeClass(COMPACT_SCREEN_MAX_WIDTH)

    /**
     * Small tablet, tablet in portrait or foldable in portrait (unfolded).
     */
    data object Medium : WindowSizeClass(MEDIUM_SCREEN_MAX_WIDTH)

    /**
     * Phone in landscape, tablet in landscape, foldable in landscape, desktop and ultra-wide.
     */
    data object ExpandedAndBigger : WindowSizeClass(Int.MAX_VALUE)

    companion object {
        private const val COMPACT_SCREEN_MAX_WIDTH = 600
        private const val MEDIUM_SCREEN_MAX_WIDTH = 840
    }
}

fun Context.getColorCompat(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.copyToClipboard(label: String, text: String) {
    with(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager) {
        setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

fun Context.getCurrentProcessName() =
    if (SystemVersionUtils.isAtLeastP()) {
        Application.getProcessName()
    } else {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
    }

@Suppress("DEPRECATION")
val Context.physicalScreenHeightInPx: Int
    @SuppressLint("NewApi")
    get() = run {
        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (SystemVersionUtils.isAtLeastR()) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.height()
        } else {
            val size = Point()
            display?.getSize(size)
            size.y
        }
    }
