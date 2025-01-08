package com.woocommerce.android.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.BuildConfig

fun NavController.navigateSafely(
    directions: NavDirections,
    extras: FragmentNavigator.Extras? = null,
    navOptions: NavOptions? = null
) {
    navigateSafelyInternal(
        actionId = directions.actionId,
        arguments = directions.arguments,
        navOptions = navOptions,
        extras = extras
    )
}

fun NavController.navigateSafely(
    @IdRes resId: Int,
    bundle: Bundle? = null,
    navOptions: NavOptions? = null
) {
    if (currentDestination?.id != resId) {
        navigateSafelyInternal(
            actionId = resId,
            arguments = bundle,
            navOptions = navOptions
        )
    }
}

private fun NavController.navigateSafelyInternal(
    actionId: Int,
    arguments: Bundle?,
    navOptions: NavOptions?,
    extras: FragmentNavigator.Extras? = null
) {
    try {
        navigate(actionId, arguments, navOptions, extras)
    } catch (e: IllegalArgumentException) {
        if (BuildConfig.DEBUG) {
            throw e
        } else {
            WooLog.w(WooLog.T.UTILS, e.toString())
            crashLogging?.recordException(e)
        }
    }
}

private val NavController.crashLogging: CrashLogging?
    get() = (context.applicationContext as? WooCommerce)?.appInitializer?.get()?.crashLogging
