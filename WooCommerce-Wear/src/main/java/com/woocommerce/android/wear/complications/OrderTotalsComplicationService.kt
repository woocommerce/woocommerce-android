package com.woocommerce.android.wear.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.woocommerce.android.R

class OrderTotalsComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                context = applicationContext,
                content = getString(R.string.order_totals_complication_preview_value),
                description = getString(R.string.order_totals_complication_preview_description)
            )

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                context = applicationContext,
                content = "$42k",
                description = "Displays the total value of the current order"
            )

            else -> null
        }
    }
}
