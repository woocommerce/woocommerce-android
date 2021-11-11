package com.woocommerce.android.util

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationApiResponse
import java.io.UnsupportedEncodingException
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import javax.inject.Inject

class NotificationsParser @Inject constructor(private val base64Decoder: Base64Decoder) {
    companion object {
        const val PUSH_ARG_NOTE_FULL_DATA = "note_full_data"
        private const val MAX_PAYLOAD_SIZE = 4096
    }

    private val gson: Gson by lazy { Gson() }

    /**
     * Builds a [NotificationModel] from a push notification payload.
     */
    fun buildNotificationModelFromPayloadMap(dataMap: Map<String, String>): NotificationModel? {
        return dataMap[PUSH_ARG_NOTE_FULL_DATA]?.let {
            getNotificationJsonFromBase64EncodedData(it)?.let { json ->
                val apiResponse = gson.fromJson(json, NotificationApiResponse::class.java)
                NotificationApiResponse.notificationResponseToNotificationModel(apiResponse)
            }
        }
    }

    /**
     * Takes a base64 encoded string and attempts to decode and parse out the containing notification object.
     */
    @Synchronized private fun getNotificationJsonFromBase64EncodedData(base64FullNoteData: String): JsonObject? {
        val b64DecodedPayload = base64Decoder.decode(base64FullNoteData, Base64.DEFAULT)

        // Decompress the payload
        val decompresser = Inflater()
        decompresser.setInput(b64DecodedPayload, 0, b64DecodedPayload.size)
        val result = ByteArray(MAX_PAYLOAD_SIZE) // max length an Android PN payload can have
        val resultLength = try {
            decompresser.inflate(result).also { decompresser.end() }
        } catch (e: DataFormatException) {
            WooLog.e(T.NOTIFICATIONS, "Can't decompress the PN Payload. It could be > 4K", e)
            0
        }

        // Attempt to parse into a String
        return runCatching {
            val json = String(result, 0, resultLength, Charsets.UTF_8)
            // Get jsonObject from the string
            val jsonObject = gson.fromJson(json, JsonObject::class.java)
            // Attempt to pull out the notification object
            if (jsonObject.has("notes")) {
                val jsonArray = jsonObject.getAsJsonArray("notes")
                if (jsonArray.size() == 1) {
                    return@runCatching jsonArray[0].asJsonObject
                }
            }
            return@runCatching null
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                val message = when (e) {
                    is UnsupportedEncodingException -> "Notification data contains non UTF8 characters"
                    else -> "Can't parse the Note JSON received in the PN"
                }
                WooLog.e(T.NOTIFICATIONS, message, e)
                null
            }
        )
    }
}
