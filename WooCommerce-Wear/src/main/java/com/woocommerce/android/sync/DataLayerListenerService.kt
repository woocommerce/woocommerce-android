package com.woocommerce.android.sync

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

private const val TAG = "DataLayerSample"
private const val START_ACTIVITY_PATH = "/auth"
private const val DATA_ITEM_RECEIVED_PATH = "/data-item-received"

class DataLayerListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("DATA RECEIVED", "onDataChanged: $dataEvents")

        // Loop through the events and send a message
        // to the node that created the data item.
        dataEvents.map { it.dataItem.uri }
            .forEach { uri ->
                // Get the node ID from the host value of the URI.
                val nodeId: String = uri.host.orEmpty()
                // Set the data of the message to be the bytes of the URI.
                val payload: ByteArray = uri.toString().toByteArray()

                // Send the RPC.
                Wearable.getMessageClient(this)
                    .sendMessage(nodeId, DATA_ITEM_RECEIVED_PATH, payload)
            }
    }
}
