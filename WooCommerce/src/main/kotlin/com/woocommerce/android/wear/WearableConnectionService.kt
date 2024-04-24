package com.woocommerce.android.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.woocommerce.android.wear.WearableConnectionRepository.MessagePath.AUTH
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearableConnectionService : WearableListenerService() {

    @Inject
    lateinit var connRepository: WearableConnectionRepository

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged")
    }

    override fun onMessageReceived(message: MessageEvent) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived: ${message.path}")
        connRepository.sendDataToAllNodes(
            path = AUTH,
            data = DataMap().apply { putString("token", "a-test-token") }
        )
        Log.d(TAG, "${message.path} replied")
    }

    companion object {
        private const val TAG = "WearableConnectionService"
    }
}
