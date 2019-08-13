package com.woocommerce.android.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhaarman.mockitokotlin2.mock
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionChangeReceiver @Inject constructor() : BroadcastReceiver() {
    companion object {
        private var isFirstReceive = true
        private var wasConnected = true

        private var mockBus: EventBus = mock()
        fun getEventBus() = mockBus
    }

    class ConnectionChangeEvent(var isConnected: Boolean)

    override fun onReceive(context: Context, intent: Intent) {
        // mocked - do nothing
    }
}
