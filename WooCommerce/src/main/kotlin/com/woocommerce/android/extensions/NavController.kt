package com.woocommerce.android.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.BuildConfig

fun NavController.navigateSafely(
    directions: NavDirections,
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

    navigateSafelyInternal(directions, navOptions, extras)
}

fun NavController.navigateSafely(
    @IdRes resId: Int,
    bundle: Bundle? = null,
    navOptions: NavOptions? = null
) {
    if (currentDestination?.id != resId) {
            navigate(resId, bundle, navOptions)
        }
}
