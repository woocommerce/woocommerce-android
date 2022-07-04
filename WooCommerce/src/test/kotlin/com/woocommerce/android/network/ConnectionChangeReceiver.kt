package com.woocommerce.android.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.greenrobot.eventbus.EventBus
import org.mockito.kotlin.mock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionChangeReceiver @Inject constructor() : BroadcastReceiver() {
    companion object {
        private var mockBus: EventBus = mock()
        fun getEventBus() = mockBus
    }

    class ConnectionChangeEvent(var isConnected: Boolean)

    override fun onReceive(context: Context, intent: Intent) {
        // mocked - do nothing
    }
}
