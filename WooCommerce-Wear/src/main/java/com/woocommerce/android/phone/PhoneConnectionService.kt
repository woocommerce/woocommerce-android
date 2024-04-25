package com.woocommerce.android.phone

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneConnectionService : WearableListenerService() {

    @Inject lateinit var phoneConnectionRepository: PhoneConnectionRepository

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d("DATA RECEIVED", "onDataChanged")
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach { phoneConnectionRepository.handleReceivedData(it.dataItem) }
    }
}
