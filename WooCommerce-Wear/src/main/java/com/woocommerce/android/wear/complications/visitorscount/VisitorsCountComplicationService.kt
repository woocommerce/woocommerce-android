package com.woocommerce.android.wear.complications.visitorscount

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.woocommerce.android.R
import com.woocommerce.android.wear.complications.createTextComplicationData

class VisitorsCountComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                context = applicationContext,
                content = getString(R.string.visitors_count_complication_preview_value),
                description = getString(R.string.visitors_count_complication_preview_description)
            )

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        TODO("Not yet implemented")
    }
}
