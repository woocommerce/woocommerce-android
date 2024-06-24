package com.woocommerce.android.wear.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.woocommerce.android.R
import com.woocommerce.android.app.WearMainActivity

class WooWearComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        Log.d("WooWearComplicationService", "getPreviewData: $type")
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                createTextComplicationData(
                    text = "5",
                    contentDescription = getString(R.string.orders_complication_preview_description)
                )
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> createImageComplicationData(
                contentDescription = getString(R.string.icon_complication_preview_description)
            )

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d("WooWearComplicationService", "onComplicationRequest: ${request.complicationType}")
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createTextComplicationData(
                text = "10", // get Today's Orders count
                contentDescription = getString(R.string.orders_complication_preview_description)
            )

            ComplicationType.MONOCHROMATIC_IMAGE -> createImageComplicationData(
                contentDescription = getString(R.string.icon_complication_preview_description)
            )

            else -> null
        }
    }

    private fun createTextComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
            .setMonochromaticImage(createIcon())
            .setTapAction(createAppPendingIntent())
            .build()

    private fun createImageComplicationData(
        contentDescription: String
    ) = MonochromaticImageComplicationData.Builder(
        monochromaticImage = createIcon(),
        contentDescription = PlainComplicationText.Builder(contentDescription).build()
    ).setTapAction(createAppPendingIntent()).build()

    private fun createAppPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, WearMainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }

    private fun createIcon() = MonochromaticImage.Builder(
        Icon.createWithResource(applicationContext, R.drawable.img_woo_bubble_white),
    ).build()
}
