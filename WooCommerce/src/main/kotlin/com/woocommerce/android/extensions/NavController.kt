package com.woocommerce.android.extensions

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * Prevents crashes caused by rapidly double-clicking views which navigate to the same
 * destination twice
 */
fun NavController.navigateSafely(directions: NavDirections) {
    currentDestination?.getAction(directions.actionId)?.let { navigate(directions) }
}

fun NavController.navigateSafely(@IdRes resId: Int) {
    if (currentDestination?.id == resId == false) {
        navigate(resId, null)
    }
}
