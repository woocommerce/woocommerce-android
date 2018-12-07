package com.woocommerce.android.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.NotificationsUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

object NotificationHandler {
    private val ACTIVE_NOTIFICATIONS_MAP = mutableMapOf<Int, Bundle>()

    private const val NOTIFICATION_GROUP_KEY = "notification_group_key"
    private const val PUSH_NOTIFICATION_ID = 10000
    const val GROUP_NOTIFICATION_ID = 30000
    private const val MAX_INBOX_ITEMS = 5

    private const val PUSH_ARG_USER = "user"
    private const val PUSH_ARG_TYPE = "type"
    private const val PUSH_ARG_TITLE = "title"
    private const val PUSH_ARG_MSG = "msg"
    private const val PUSH_ARG_NOTE_ID = "note_id"

    private const val PUSH_TYPE_COMMENT = "c"
    private const val PUSH_TYPE_NEW_ORDER = "store_order"

    @Synchronized fun hasNotifications() = !ACTIVE_NOTIFICATIONS_MAP.isEmpty()

    @Synchronized fun clearNotifications() {
        ACTIVE_NOTIFICATIONS_MAP.clear()
    }

    @Synchronized fun removeNotification(localPushId: Int) {
        ACTIVE_NOTIFICATIONS_MAP.remove(localPushId)
    }

    /**
     * This is here to simplify testing notifications
     */
    fun testNotification(context: Context, title: String, message: String, account: AccountModel) {
        val data = Bundle()
        data.putString(PUSH_ARG_TYPE, PUSH_TYPE_NEW_ORDER)
        data.putString(PUSH_ARG_TITLE, title)
        data.putString(PUSH_ARG_MSG, message)
        data.putString(PUSH_ARG_USER, account.userId.toString())
        buildAndShowNotificationFromNoteData(context, data, account)
    }

    fun buildAndShowNotificationFromNoteData(context: Context, data: Bundle, account: AccountModel) {
        if (data.isEmpty) {
            WooLog.e(T.NOTIFS, "Push notification received without a valid Bundle!")
            return
        }

        val wpComNoteId = data.getString(PUSH_ARG_NOTE_ID, "")
        // TODO Temporarily disabled so it's easier to test spoofed notifications, restore
//        if (wpComNoteId.isNullOrEmpty()) {
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

        // TODO: Store note object in database

        val noteType = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE))

        // skip if user chose to disable this type of notification
        if ((noteType == PUSH_TYPE_NEW_ORDER && !AppPrefs.isOrderNotificationsEnabled()) ||
                (noteType == PUSH_TYPE_COMMENT && !AppPrefs.isReviewNotificationsEnabled())) {
            WooLog.i(T.NOTIFS, "Skipped $noteType notification")
            return
        }

        val title = if (noteType == PUSH_TYPE_NEW_ORDER) {
            // New order notifications have title 'WordPress.com' - just show the app name instead
            // TODO Consider revising this, perhaps use the contents of the note as the title/body of the notification
            context.getString(R.string.app_name)
        } else {
            StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE))
                    ?: context.getString(R.string.app_name)
        }

        val message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG))

        val localPushId = getLocalPushIdForWpComNoteId(wpComNoteId)
        ACTIVE_NOTIFICATIONS_MAP[localPushId] = data

        if (NotificationsUtils.isNotificationsEnabled(context)) {
            bumpPushNotificationsAnalytics(context, Stat.PUSH_NOTIFICATION_RECEIVED, data)
            AnalyticsTracker.flush()
        }

        // Build the new notification, add group to support wearable stacking
        val builder = getNotificationBuilder(context, title, message)
        val largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"),
                shouldCircularizeNoteIcon(noteType))
        largeIconBitmap?.let { builder.setLargeIcon(it) }

        showSingleNotificationForBuilder(context, builder, noteType, wpComNoteId, localPushId, true)

        // Also add a group summary notification, which is required for non-wearable devices
        // Do not need to play the sound again. We've already played it in the individual builder.
        showGroupNotificationForBuilder(context, builder, wpComNoteId, message)
    }

    /**
     * For a given remote note ID, return a unique local ID to track that notification with, or return
     * the existing local ID if a notification matching the remote note ID is already being displayed.
     */
    private fun getLocalPushIdForWpComNoteId(wpComNoteId: String): Int {
        // Update notification content for the same noteId if it is already showing
        for (id in ACTIVE_NOTIFICATIONS_MAP.keys) {
            val noteBundle = ACTIVE_NOTIFICATIONS_MAP[id]
            if (noteBundle?.getString(PUSH_ARG_NOTE_ID, "") == wpComNoteId) {
                return id
            }
        }

        // Notification isn't already showing
        return PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size
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
     * Returns true if the note type is known to have a Gravatar.
     */
    private fun shouldCircularizeNoteIcon(noteType: String): Boolean {
        if (noteType.isEmpty()) return false

        return when (noteType) {
            PUSH_TYPE_COMMENT -> true
            else -> false
        }
    }

    private fun getNotificationBuilder(context: Context, title: String, message: String?): NotificationCompat.Builder {
        return NotificationCompat.Builder(context,
                context.getString(R.string.notification_channel_general_id))
                .setSmallIcon(R.drawable.ic_woo_w_notification)
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
        noteType: String,
        wpComNoteId: String,
        pushId: Int,
        notifyUser: Boolean
    ) {
        when (noteType) {
            PUSH_TYPE_NEW_ORDER -> {
                if (AppPrefs.isOrderNotificationsChaChingEnabled()) {
                    builder.setSound(Uri.parse("file:///android_asset/WooChaChing.wav"))
                } else {
                    builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
                }
            }
            PUSH_TYPE_COMMENT -> {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
                // TODO: Add quick actions for comments
            }
        }

        showWPComNotificationForBuilder(builder, context, wpComNoteId, pushId, notifyUser)
    }

    private fun showGroupNotificationForBuilder(
        context: Context,
        builder: NotificationCompat.Builder,
        wpComNoteId: String,
        message: String?
    ) {
        // Using a copy of the map to avoid concurrency problems
        val notesMap = ACTIVE_NOTIFICATIONS_MAP.toMap()
        if (notesMap.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()

            var noteCounter = 1
            for (pushBundle in notesMap.values) {
                // InboxStyle notification is limited to 5 lines
                if (noteCounter > MAX_INBOX_ITEMS) break

                // Skip notes with no content from the 5-line inbox
                if (pushBundle.getString(PUSH_ARG_MSG) == null) continue

                if (pushBundle.getString(PUSH_ARG_TYPE, "") == PUSH_TYPE_COMMENT) {
                    val pnTitle = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_TITLE))
                    val pnMessage = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_MSG))
                    inboxStyle.addLine("$pnTitle: $pnMessage")
                } else {
                    val pnMessage = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_MSG))
                    inboxStyle.addLine(pnMessage)
                }

                noteCounter++
            }

            if (notesMap.size > MAX_INBOX_ITEMS) {
                inboxStyle.setSummaryText(
                        String.format(context.getString(R.string.more_notifications), notesMap.size - MAX_INBOX_ITEMS)
                )
            }

            val subject = String.format(context.getString(R.string.new_notifications), notesMap.size)
            val groupBuilder = NotificationCompat.Builder(
                    context,
                    context.getString(R.string.notification_channel_general_id))
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    .setSmallIcon(R.drawable.ic_woo_w_notification)
                    .setColor(ContextCompat.getColor(context, R.color.wc_purple))
                    .setGroup(NOTIFICATION_GROUP_KEY)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setTicker(message)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(subject)
                    .setStyle(inboxStyle)

            showWPComNotificationForBuilder(groupBuilder, context, wpComNoteId, GROUP_NOTIFICATION_ID, false)
        } else {
            // Set the individual notification we've already built as the group summary
            builder.setGroupSummary(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            showWPComNotificationForBuilder(builder, context, wpComNoteId, GROUP_NOTIFICATION_ID, false)
        }
    }

    /**
     * Creates a notification for a WordPress.com note, attaching an intent for the note's tap action.
     */
    private fun showWPComNotificationForBuilder(
        builder: NotificationCompat.Builder,
        context: Context,
        wpComNoteId: String,
        pushId: Int,
        notifyUser: Boolean
    ) {
        // TODO Create an Intent containing the wpComNoteId that launches the MainActivity to handle the tap action
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

        // Call processing service when notification is dismissed
        val pendingDeleteIntent = NotificationsProcessingService.getPendingIntentForNotificationDismiss(context, pushId)
        builder.setDeleteIntent(pendingDeleteIntent)

        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)

        val pendingIntent = PendingIntent.getActivity(context, pushId, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(pushId, builder.build())
    }

    /**
     * Attach default properties and track given analytics for the given notifications-related [stat].
     *
     * Will skip tracking if user has disabled notifications from being shown at the app system settings level.
     */
    private fun bumpPushNotificationsAnalytics(context: Context, stat: Stat, noteBundle: Bundle) {
        if (!NotificationsUtils.isNotificationsEnabled(context)) return

        val wpComNoteId = noteBundle.getString(PUSH_ARG_NOTE_ID, "")
        if (wpComNoteId.isNotEmpty()) {
            val properties = mutableMapOf<String, Any>()
            properties["notification_note_id"] = wpComNoteId

            noteBundle.getString(PUSH_ARG_TYPE)?.takeUnless { it.isEmpty() }?.let { noteType ->
                // 'comment' types are sent in PN as type = "c"
                properties["notification_type"] = if (noteType == PUSH_TYPE_COMMENT) "comment" else noteType
            }

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val latestGCMToken = preferences.getString(FCMRegistrationIntentService.WPCOM_PUSH_DEVICE_TOKEN, null)
            properties["push_notification_token"] = latestGCMToken ?: ""
            AnalyticsTracker.track(stat, properties)
        }
    }
}
