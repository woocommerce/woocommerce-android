package com.woocommerce.android.ui.woopos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ConnectionType {
    WIFI,
    CELLULAR,
    UNKNOWN
}

@Singleton
class WooPosConnectionTypeProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getConnectionType(): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCapabilities = connectivityManager
            ?.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }

        return when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
            else -> ConnectionType.UNKNOWN
        }
    }
}
