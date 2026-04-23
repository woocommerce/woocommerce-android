package com.woocommerce.android.ui.woopos.util

import android.content.Context
import dagger.Reusable
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

@Reusable
class WooPosNetworkStatus @Inject constructor(
    private val context: Context
) {
    fun isConnected() = NetworkUtils.isNetworkAvailable(context)
}
