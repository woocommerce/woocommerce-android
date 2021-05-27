package com.woocommerce.android.util

import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import javax.inject.Inject

class LocationUtils @Inject constructor(private val appContext: Context) {
    fun isLocationEnabled(): Boolean {
        return (appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.let {
            return LocationManagerCompat.isLocationEnabled(it)
        } ?: false
    }
}
