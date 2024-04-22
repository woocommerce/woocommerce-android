package com.woocommerce.android.sync

import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ConnectWearableUseCase @Inject constructor() :
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {
    private val _events = MutableStateFlow(mutableListOf<WearableConnectionEvent>())
    val events: Flow<List<WearableConnectionEvent>> = _events

    operator fun invoke(
        dataClient: DataClient,
        messageClient: MessageClient,
        capabilityClient: CapabilityClient
    ) {
        dataClient.addListener(this)
        messageClient.addListener(this)
        capabilityClient.addListener(
            this,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )
    }

    override fun onDataChanged(data: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: $data")
        _events.update {
            it.apply { add(WearableConnectionEvent(data.toString())) }
        }
    }

    override fun onMessageReceived(message: MessageEvent) {
        Log.d(TAG, "onMessageReceived: $message")
        _events.update {
            it.apply { add(WearableConnectionEvent(message.toString())) }
        }
    }

    override fun onCapabilityChanged(info: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: $info")
        _events.update {
            it.apply { add(WearableConnectionEvent(info.toString())) }
        }
    }

    data class WearableConnectionEvent(
        val content: String
    )

    companion object {
        const val TAG = "ConnectWearableUseCase"
    }
}
