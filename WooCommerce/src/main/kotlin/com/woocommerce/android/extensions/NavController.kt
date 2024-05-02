package com.woocommerce.android.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.BuildConfig

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
    extras: FragmentNavigator.Extras? = null,
    navOptions: NavOptions? = null
) {
    fun navigateSafelyInternal(
        directions: NavDirections,
        navOptions: NavOptions?,
        extras: FragmentNavigator.Extras?
    ) {
        try {
            navigate(directions.actionId, directions.arguments, navOptions, extras)
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG) {
                throw e
            } else {
                WooLog.w(WooLog.T.UTILS, e.toString())
            }
        }
    }

    if (skipThrottling) {
        navigateSafelyInternal(directions, navOptions, extras)
    } else {
        CallThrottler.throttle {
            navigateSafelyInternal(directions, navOptions, extras)
        }
    }
}

fun NavController.navigateSafely(
    @IdRes resId: Int,
    bundle: Bundle? = null,
    navOptions: NavOptions? = null
) {
    CallThrottler.throttle {
        if (currentDestination?.id != resId) {
            navigate(resId, bundle, navOptions)
        }
    }
}

fun NavController.popBackStackSafely() {
    CallThrottler.throttle {
        if (popBackStack().not()) {
            WooLog.w(WooLog.T.UTILS, "No back stack entry found to pop.")
        }
    }
}
