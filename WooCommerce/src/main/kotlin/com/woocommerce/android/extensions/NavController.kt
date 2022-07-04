package com.woocommerce.android.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigator

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

fun NavController.navigateSafely(
    directions: NavDirections,
    skipThrottling: Boolean = false,
    extras: FragmentNavigator.Extras? = null
) {
    if (skipThrottling) {
        currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
    } else {
        CallThrottler.throttle {
            currentDestination?.getAction(directions.actionId)?.let {
                if (extras != null) {
                    navigate(directions, extras)
                } else {
                    navigate(directions)
                }
            }
        }
    }
}

fun NavController.navigateSafely(@IdRes resId: Int, bundle: Bundle? = null) {
    CallThrottler.throttle {
        if (currentDestination?.id != resId) {
            navigate(resId, bundle)
        }
    }
}
