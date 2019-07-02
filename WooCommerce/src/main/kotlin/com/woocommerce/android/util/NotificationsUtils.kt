package com.woocommerce.android.util

import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.util.WooLog.T
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationApiResponse
import java.io.UnsupportedEncodingException
import java.util.zip.DataFormatException
import java.util.zip.Inflater

object NotificationsUtils {
    /**
     * Checks if global notifications toggle is enabled in the Android app settings.
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }

    /**
     * Builds a [NotificationModel] from a push notification bundle.
     */
    fun buildNotificationModelFromBundle(data: Bundle): NotificationModel? {
        return data.getString(NotificationHandler.PUSH_ARG_NOTE_FULL_DATA)?.let {
            getNotificationJsonFromBase64EncodedData(it)?.let { json ->
            val apiResponse = Gson().fromJson(json.toString(), NotificationApiResponse::class.java)
            NotificationApiResponse.notificationResponseToNotificationModel(apiResponse)
        } }
    }

    /**
     * Takes a base64 encoded string and attempts to decode and parse out the containing notification object.
     */
    @Synchronized fun getNotificationJsonFromBase64EncodedData(base64FullNoteData: String): JSONObject? {
        val b64DecodedPayload = Base64.decode(base64FullNoteData, Base64.DEFAULT)

        // Decompress the payload
        val decompresser = Inflater()
        decompresser.setInput(b64DecodedPayload, 0, b64DecodedPayload.size)
        val result = ByteArray(4096) // max length an Android PN payload can have
        val resultLength = try {
            decompresser.inflate(result).also { decompresser.end() }
        } catch (e: DataFormatException) {
            WooLog.e(T.NOTIFICATIONS, "Can't decompress the PN Payload. It could be > 4K", e)
            0
        }

        var resultJson: JSONObject? = null

        // Attempt to parse into a String
        try {
            String(result, 0, resultLength, Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            WooLog.e(T.NOTIFICATIONS, "Notification data contains non UTF8 characters.", e)
            null
        }?.let { out ->
            try {
                // Get jsonObject from the string
                val jsonObject = JSONObject(out)
                // Attempt to pull out the notification object
                if (jsonObject.has("notes")) {
                    val jsonArray = jsonObject.getJSONArray("notes")
                    if (jsonArray != null && jsonArray.length() == 1) {
                        resultJson = jsonArray.getJSONObject(0)
                    }
                }
            } catch (e: JSONException) {
                WooLog.e(T.NOTIFICATIONS, "Can't parse the Note JSON received in the PN", e)
            }
        }

        return resultJson
    }
}
