package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.woocommerce.commons.wear.DataParameters.TIMESTAMP
import com.woocommerce.commons.wear.DataPath
import java.time.Instant
import javax.inject.Inject

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient
) {
    fun sendDataToAllNodes(
        path: DataPath,
        data: DataMap = DataMap()
    ) {
        PutDataMapRequest
            .create(path.value)
            .addData(data)
            .asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }

    private fun PutDataMapRequest.addData(data: DataMap) = apply {
        dataMap.putAll(data)
        dataMap.putLong(TIMESTAMP.value, Instant.now().epochSecond)
    }
}
