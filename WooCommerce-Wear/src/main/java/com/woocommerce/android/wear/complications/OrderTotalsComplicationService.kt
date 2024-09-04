package com.woocommerce.android.wear.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class OrderTotalsComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        TODO("Not yet implemented")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        TODO("Not yet implemented")
    }
}
