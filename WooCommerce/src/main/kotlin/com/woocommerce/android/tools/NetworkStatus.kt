package com.woocommerce.android.tools

import android.content.Context
import org.wordpress.android.util.NetworkUtils
import javax.inject.Singleton

@Singleton
class NetworkStatus(private var context: Context) {
    fun isConnected() = NetworkUtils.isNetworkAvailable(context)
}
