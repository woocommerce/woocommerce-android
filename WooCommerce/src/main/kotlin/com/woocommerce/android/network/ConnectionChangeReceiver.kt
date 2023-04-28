package com.woocommerce.android.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.ConnectivityObserver
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global network connection change receiver - The implementing Application must register this receiver in either it's
 * Application class (if it wants to manage connectivity status globally) or inside the activity that wishes to
 * monitor connectivity.
 */
@Singleton
class ConnectionChangeReceiver @Inject constructor(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val connectivityObserver: ConnectivityObserver
) : BroadcastReceiver() {
    companion object {
        private var isFirstReceive = true
        private var wasConnected = true

        fun getEventBus() = EventBus.getDefault()
    }

    class ConnectionChangeEvent(var isConnected: Boolean)

    /**
     * Listens for device connection changes.
     *
     * This method is called whenever something about the device connection has changed, not just
     * it's network connectivity. Only dispatch a [ConnectionChangeEvent] event
     * when connection availability has changed.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val isConnected = NetworkUtils.isNetworkAvailable(context)
        appScope.launch {
            connectivityObserver.update(isConnected)
        }
        if (isFirstReceive || isConnected != wasConnected) {
            WooLog.i(T.DEVICE, "Connection Changed to $isConnected")
            wasConnected = isConnected
            isFirstReceive = false
            getEventBus().post(ConnectionChangeEvent(isConnected))
        }
    }
}
