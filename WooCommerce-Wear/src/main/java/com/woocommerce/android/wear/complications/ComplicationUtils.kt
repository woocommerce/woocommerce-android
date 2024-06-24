package com.woocommerce.android.wear.complications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import com.woocommerce.android.R
import com.woocommerce.android.app.WearMainActivity

fun createTextComplicationData(
    context: Context,
    content: String,
    description: String
) = ShortTextComplicationData.Builder(content.toComplicationText(), description.toComplicationText())
    .setMonochromaticImage(createIcon(context))
    .setTapAction(createAppPendingIntent(context))
    .build()

fun createImageComplicationData(
    context: Context,
    contentDescription: String
) = MonochromaticImageComplicationData.Builder(
    monochromaticImage = createIcon(context),
    contentDescription = PlainComplicationText.Builder(contentDescription).build()
).setTapAction(createAppPendingIntent(context)).build()

private fun createAppPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, WearMainActivity::class.java)
    val flags = PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getActivity(context, 0, intent, flags)
}

private fun createIcon(context: Context) = MonochromaticImage.Builder(
    Icon.createWithResource(context, R.drawable.img_woo_bubble_white),
).build()

private fun String.toComplicationText() = PlainComplicationText.Builder(this).build()
