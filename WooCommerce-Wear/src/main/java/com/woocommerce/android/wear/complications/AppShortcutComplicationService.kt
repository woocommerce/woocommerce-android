package com.woocommerce.android.wear.complications

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.woocommerce.android.R

class AppShortcutComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.MONOCHROMATIC_IMAGE -> createImageComplicationData(
                context = applicationContext,
                contentDescription = getString(R.string.shortcut_complication_preview_description)
            )

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return when (request.complicationType) {
            ComplicationType.MONOCHROMATIC_IMAGE -> createImageComplicationData(
                context = applicationContext,
                contentDescription = getString(R.string.shortcut_complication_preview_description)
            )

            else -> null
        }
    }
}
