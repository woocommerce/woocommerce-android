package com.woocommerce.android.extensions

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

/**
 * If a user accidentally navigates twice quickly (for example, tapping a button twice), the app may
 * crash with "IllegalArgumentException: navigation destination is unknown to this NavController."
 * We prevent this by catching and ignoring the exception.
 *
 * https://github.com/woocommerce/woocommerce-android/issues/1719
 */
fun NavController.navigateSafely(directions: NavDirections) {
    try {
        navigate(directions)
    } catch (e: IllegalArgumentException) {
        WooLog.e(T.UTILS, e)
    }
}

fun NavController.navigateSafely(@IdRes resId: Int) {
    try {
        navigate(resId, null)
    } catch (e: IllegalArgumentException) {
        WooLog.e(T.UTILS, e)
    }
}
