package com.woocommerce.android.extensions

import android.util.Log
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * Prevents crashes caused by rapidly double-clicking views which navigate to the same
 * destination twice
 */
object CallThrottler {
    private const val DELAY = 200
    private var lastTime: Long = 0

    fun throttle(call: () -> Unit) {
        if (System.currentTimeMillis() - lastTime > DELAY) {
            lastTime = System.currentTimeMillis()
            call()
        }
    }
}

fun NavController.navigateSafely(directions: NavDirections, skipThrottling: Boolean = false) {
    if (skipThrottling) {
        currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
    } else {
        CallThrottler.throttle {
            currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
        }
    }
}

fun NavController.navigateSafely(@IdRes resId: Int) {
    CallThrottler.throttle {
        if (currentDestination?.id != resId) {
            navigate(resId, null)
        }
    }
}
