package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import javax.inject.Inject

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient
) {
    private fun sendDataToAllNodes(
        path: MessagePath,
        data: DataMap = DataMap()
    ) {
        PutDataMapRequest
            .create(path.value)
            .apply { dataMap.putAll(data) }
            .asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }

    enum class MessagePath(val value: String) {
        AUTH("/auth")
    }
}
