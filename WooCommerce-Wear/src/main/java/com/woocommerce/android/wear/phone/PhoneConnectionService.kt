package com.woocommerce.android.wear.phone

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
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach { phoneConnectionRepository.handleReceivedData(it.dataItem) }
    }
}
