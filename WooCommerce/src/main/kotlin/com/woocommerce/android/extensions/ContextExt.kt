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
import com.woocommerce.android.R
import com.woocommerce.android.util.SystemVersionUtils

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

val Context.isDisplaySmallerThan720: Boolean
    get() = !resources.getBoolean(R.bool.is_at_least_720sw)
