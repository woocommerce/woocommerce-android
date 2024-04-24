package com.woocommerce.android.phone

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneConnectionService : WearableListenerService() {

    @Inject lateinit var connRepository: PhoneConnectionRepository

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d("DATA RECEIVED", "onDataChanged")
        dataEvents.forEach { connRepository.handleReceivedData(it) }
    }

    override fun onMessageReceived(message: MessageEvent) {
        super.onMessageReceived(message)
        Log.d("MESSAGE RECEIVED", "onMessageReceived")
    }
}
