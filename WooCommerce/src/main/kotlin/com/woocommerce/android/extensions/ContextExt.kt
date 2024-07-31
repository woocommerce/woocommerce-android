package com.woocommerce.android.extensions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.os.Parcelable
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.woocommerce.android.util.SystemVersionUtils
import kotlinx.parcelize.Parcelize

val Context.windowSizeClass: WindowSizeClass
    get() = determineWindowWidthSizeClassByGivenSize(resources.configuration.screenWidthDp)

val Context.windowHeightSizeClass: WindowSizeClass
    get() = determineWindowHeightSizeClassByGivenSize(resources.configuration.screenHeightDp)

private fun determineWindowWidthSizeClassByGivenSize(sizeDp: Int): WindowSizeClass {
    return when {
        sizeDp < WindowSizeClass.Compact.maxWidthDp -> WindowSizeClass.Compact
        sizeDp < WindowSizeClass.Medium.maxWidthDp -> WindowSizeClass.Medium
        else -> WindowSizeClass.ExpandedAndBigger
    }
}

private fun determineWindowHeightSizeClassByGivenSize(sizeDp: Int): WindowSizeClass {
    return when {
        sizeDp < WindowSizeClass.Compact.maxHeightDp -> WindowSizeClass.Compact
        sizeDp < WindowSizeClass.Medium.maxHeightDp -> WindowSizeClass.Medium
        else -> WindowSizeClass.ExpandedAndBigger
    }
}

/**
 * Window size class type based on Material Design
 * [guidelines](https://m3.material.io/foundations/layout/applying-layout/window-size-classes)
 */
@Parcelize
sealed class WindowSizeClass(val maxWidthDp: Int, val maxHeightDp: Int) : Parcelable, Comparable<WindowSizeClass> {
    /**
     * Width: 99.96% of phones in portrait.
     * Height: 99.78% of phones in landscape.
     */
    data object Compact : WindowSizeClass(COMPACT_SCREEN_MAX_WIDTH, COMPACT_SCREEN_MAX_HEIGHT)

    /**
     * Width: 93.73% of tablets in portrait, most large unfolded inner displays in portrait.
     * Height: 96.56% of tablets in landscape, 97.59% of phones in portrait.
     */
    data object Medium : WindowSizeClass(MEDIUM_SCREEN_MAX_WIDTH, MEDIUM_SCREEN_MAX_HEIGHT)

    /**
     * Width: 97.22% of tablets in landscape, most large unfolded inner displays in landscape.
     * Height: 94.25% of tablets in portrait.
     */
    data object ExpandedAndBigger : WindowSizeClass(EXPANDED_SCREEN_MAX_WIDTH, EXPANDED_SCREEN_MAX_HEIGHT)

    companion object {
        private const val COMPACT_SCREEN_MAX_WIDTH = 600
        private const val MEDIUM_SCREEN_MAX_WIDTH = 840
        private const val EXPANDED_SCREEN_MAX_WIDTH = 1200

        private const val COMPACT_SCREEN_MAX_HEIGHT = 480
        private const val MEDIUM_SCREEN_MAX_HEIGHT = 900
        private const val EXPANDED_SCREEN_MAX_HEIGHT = 2000
    }

    override fun compareTo(other: WindowSizeClass): Int {
        return this.maxWidthDp.compareTo(other.maxWidthDp)
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
