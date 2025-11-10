package com.woocommerce.android.ui.woopos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionType(val value: String) {
    WIFI("wifi"),
    CELLULAR("cellular"),
    UNKNOWN("unknown");

    override fun toString(): String = value
}

@Singleton
class WooPosConnectionTypeProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getConnectionType(): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectionType.UNKNOWN

        val activeNetwork = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return ConnectionType.UNKNOWN

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            else -> ConnectionType.UNKNOWN
        }
    }
}