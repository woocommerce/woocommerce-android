package com.woocommerce.android.system

import android.content.Context
import dagger.Reusable
import org.wordpress.android.util.NetworkUtils

@Reusable
class NetworkStatus(private val context: Context) {
    fun isConnected() = NetworkUtils.isNetworkAvailable(context)
}
