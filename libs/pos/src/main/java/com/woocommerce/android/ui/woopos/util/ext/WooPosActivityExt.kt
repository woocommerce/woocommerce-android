package com.woocommerce.android.ui.woopos.util.ext

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun Activity.lockWooPosOrientation() {
    requestedOrientation = if (isWooPosPhoneLayout()) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
}

fun Activity.setupWooPosTopAndBottomInsets(@IdRes rootViewId: Int) {
    val rootView = findViewById<View>(rootViewId)
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
        insets.toWindowInsets()?.let { windowInsets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
            val isGestureNavigation = insetsCompat.isGestureNavigation(view.context)

            val topPadding = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottomPadding = if (isGestureNavigation) {
                0
            } else {
                insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            }
            view.updatePadding(
                top = topPadding,
                bottom = bottomPadding
            )
        }
        insets
    }
}
