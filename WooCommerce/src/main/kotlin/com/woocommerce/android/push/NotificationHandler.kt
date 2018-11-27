package com.woocommerce.android.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

// TODO Largely lifted from WPAndroid's GCMMessageService with several important things omitted - should be rewritten
object NotificationHandler {
    private val ACTIVE_NOTIFICATIONS_MAP = mutableMapOf<Int, Bundle>()

    private const val NOTIFICATION_GROUP_KEY = "notification_group_key"
    private const val PUSH_NOTIFICATION_ID = 10000

    private const val PUSH_ARG_USER = "user"
    private const val PUSH_ARG_TYPE = "type"
    private const val PUSH_ARG_TITLE = "title"
    private const val PUSH_ARG_MSG = "msg"
    private const val PUSH_ARG_NOTE_ID = "note_id"

    fun buildAndShowNotificationFromNoteData(context: Context, data: Bundle, account: AccountModel) {
        if (data.isEmpty) {
            WooLog.e(T.NOTIFS, "Push notification received without a valid Bundle!")
            return
        }

        val wpcomNoteID = data.getString(PUSH_ARG_NOTE_ID, "")
        // TODO Temporarily disabled so it's easier to test spoofed notifications, restore
//        if (wpcomNoteID.isNullOrEmpty()) {
//            // At this point 'note_id' is always available in the notification bundle.
//            WooLog.e(T.NOTIFS, "Push notification received without a valid note_id in the payload!")
//            return
//        }

        val pushUserId = data.getString(PUSH_ARG_USER)
        // pushUserId is always set server side, but better to double check it here.
        if (account.userId.toString() != pushUserId) {
            WooLog.e(T.NOTIFS, "WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
            return
        }

        val noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE))

        val title = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE))
                ?: context.getString(R.string.app_name)
        val message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG))

        // Update notification content for the same noteId if it is already showing
        var localPushId = 0
        for (id in ACTIVE_NOTIFICATIONS_MAP.keys) {
            val noteBundle = ACTIVE_NOTIFICATIONS_MAP[id]
            if (noteBundle?.getString(PUSH_ARG_NOTE_ID, "") == wpcomNoteID) {
                localPushId = id
                ACTIVE_NOTIFICATIONS_MAP[localPushId] = data
                break
            }
        }

        if (localPushId == 0) {
            localPushId = PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size
            ACTIVE_NOTIFICATIONS_MAP[localPushId] = data
        }

        // Build the new notification, add group to support wearable stacking
        val builder = getNotificationBuilder(context, title, message)
        val largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"),
                shouldCircularizeNoteIcon(noteType))
        largeIconBitmap?.let { builder.setLargeIcon(it) }

        showSingleNotificationForBuilder(context, builder, wpcomNoteID, localPushId, true)

        // TODO Show group notification
    }

    private fun getLargeIconBitmap(context: Context, iconUrl: String?, shouldCircularizeIcon: Boolean): Bitmap? {
        iconUrl?.let {
            try {
                val decodedIconUrl = URLDecoder.decode(iconUrl, "UTF-8")
                val largeIconSize = context.resources.getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_height)
                val resizedUrl = PhotonUtils.getPhotonImageUrl(decodedIconUrl, largeIconSize, largeIconSize)

                val largeIconBitmap = ImageUtils.downloadBitmap(resizedUrl)
                if (largeIconBitmap != null && shouldCircularizeIcon) {
                    return ImageUtils.getCircularBitmap(largeIconBitmap)
                }

                return largeIconBitmap
            } catch (e: UnsupportedEncodingException) {
                WooLog.e(T.NOTIFS, e)
            }
        }
        return null
    }

    /**
     * Returns true if the note type is known to have a gravatar
     */
    private fun shouldCircularizeNoteIcon(noteType: String): Boolean {
        // TODO: Should declare any note types that should have circularized icons here
        return false
    }

    private fun getNotificationBuilder(context: Context, title: String, message: String?): NotificationCompat.Builder {
        return NotificationCompat.Builder(context,
                context.getString(R.string.notification_channel_general_id))
                .setSmallIcon(R.drawable.login_notification_icon)
                .setColor(ContextCompat.getColor(context, R.color.wc_purple))
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setGroup(NOTIFICATION_GROUP_KEY)
    }

    private fun showSingleNotificationForBuilder(
        context: Context,
        builder: NotificationCompat.Builder,
        wpcomNoteID: String,
        pushId: Int,
        notifyUser: Boolean
    ) {
        // TODO Create an Intent containing the wpcomNoteID that launches the MainActivity to handle the tap action
        // (and open the notifications tab)
        val resultIntent = Intent() // placeholder
        showNotificationForBuilder(builder, context, resultIntent, pushId, notifyUser)
    }

    // Displays a notification to the user
    private fun showNotificationForBuilder(
        builder: NotificationCompat.Builder,
        context: Context,
        resultIntent: Intent,
        pushId: Int,
        notifyUser: Boolean
    ) {
        // TODO This should respect user preferences, and should have sound
        if (notifyUser) {
            builder.setVibrate(longArrayOf(500, 500, 500))
            builder.setLights(-0xffff01, 1000, 5000)
        } else {
            builder.setVibrate(null)
            builder.setSound(null)
            // Do not turn the led off otherwise the previous (single) notification led is not shown.
            // We're re-using the same builder for single and group.
        }

        // TODO Call a processing service when notification is dismissed

        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)

        val pendingIntent = PendingIntent.getActivity(context, pushId, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(pushId, builder.build())
    }
}
