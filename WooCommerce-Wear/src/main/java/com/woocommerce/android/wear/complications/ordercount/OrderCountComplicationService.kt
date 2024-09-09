package com.woocommerce.android.wear.complications.ordercount

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.woocommerce.android.R
import com.woocommerce.android.wear.complications.createTextComplicationData
import com.woocommerce.android.wear.complications.ordertotals.FetchStatsForComplications
import com.woocommerce.android.wear.complications.ordertotals.FetchStatsForComplications.StatType.ORDER_COUNT
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCountComplicationService : SuspendingComplicationDataSourceService() {

    @Inject lateinit var fetchStatsForComplications: FetchStatsForComplications

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                context = applicationContext,
                content = getString(R.string.order_count_complication_preview_value),
                description = getString(R.string.order_count_complication_preview_description)
            )

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                context = applicationContext,
                content = fetchStatsForComplications(ORDER_COUNT),
                description = getString(R.string.order_count_complication_preview_description)
            )

            else -> null
        }
    }
}
