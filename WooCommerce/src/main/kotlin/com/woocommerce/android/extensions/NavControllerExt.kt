package com.woocommerce.android.extensions

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

fun NavController.navigateSafely(directions: NavDirections, extras: Navigator.Extras? = null) {
    try {
        extras?.let {
            navigate(directions, it)
        } ?: navigate(directions)
    } catch (e: IllegalArgumentException) {
        WooLog.e(T.UTILS, e)
    }
}
