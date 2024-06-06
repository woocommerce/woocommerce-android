package com.woocommerce.android.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

fun Context.isGooglePlayServicesAvailable(): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()

    return when (val connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(this)) {
        ConnectionResult.SUCCESS -> true
        else -> {
            WooLog.w(
                WooLog.T.UTILS,
                "Google Play Services unavailable, connection result: " +
                    googleApiAvailability.getErrorString(connectionResult)
            )
            return false
        }
    }
}
