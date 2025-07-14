package com.woocommerce.android.wear.system

import android.content.Context
import com.woocommerce.android.wear.ui.login.LoginRepository
import dagger.Reusable
import org.wordpress.android.util.NetworkUtils

@Reusable
class ConnectionStatus(
    private val context: Context,
    private val loginRepository: LoginRepository
) {
    fun isStoreConnected() = NetworkUtils.isNetworkAvailable(context) && loginRepository.isWPCOMSite
}
