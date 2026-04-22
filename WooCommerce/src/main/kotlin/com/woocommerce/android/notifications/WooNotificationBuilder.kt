package com.woocommerce.android.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification.FLAG_GROUP_SUMMARY
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.woocommerce.android.R
import com.woocommerce.android.extensions.loadPhotonUrlWithFallback
import com.woocommerce.android.model.Notification
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.SystemVersionUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.ImageUtils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooNotificationBuilder @Inject constructor(
    private val context: Context,
    private val notificationChannelsHandler: NotificationChannelsHandler
) {
    fun isNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }

    fun cancelNotification(pushId: Int) = NotificationManagerCompat.from(context).cancel(pushId)

    fun cancelAllNotifications() = NotificationManagerCompat.from(context).cancelAll()

    fun getActiveNotifications(): List<ActiveNotificationData> {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.map { sbn ->
            ActiveNotificationData(
                id = sbn.id,
                remoteNoteId = sbn.notification.extras.getLong(EXTRA_REMOTE_NOTE_ID),
                remoteSiteId = sbn.notification.extras.getLong(EXTRA_REMOTE_SITE_ID),
                channelType = sbn.notification.extras.getString(EXTRA_CHANNEL_TYPE),
                noteMessage = sbn.notification.extras.getString(EXTRA_NOTE_MESSAGE),
                noteTypeTrackingValue = sbn.notification.extras.getString(EXTRA_NOTE_TYPE),
                source = sbn.notification.extras.getString(EXTRA_SOURCE)
                    ?.let { runCatching { NotificationSource.valueOf(it) }.getOrNull() },
                analyticsId = sbn.notification.extras.getString(EXTRA_ANALYTICS_ID),
                isGroupSummary = (sbn.notification.flags and FLAG_GROUP_SUMMARY) != 0
            )
        }
    }

    private fun getNotificationBuilder(
        notification: Notification
    ): NotificationCompat.Builder {
        val channelId = with(notificationChannelsHandler) { notification.channelType.getChannelId() }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_woo_w_notification)
            .setColor(ContextCompat.getColor(context, R.color.color_primary))
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setGroup(notification.getGroup())
            .setContentTitle(notification.noteTitle)
            .setContentText(notification.noteMessage)
            .setTicker(notification.noteMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.noteMessage))
    }

    private fun getResultIntent(
        pushId: Int,
        notification: Notification
    ) = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(MainActivity.FIELD_OPENED_FROM_PUSH, true)
        putExtra(MainActivity.FIELD_PUSH_ID, pushId)
        putExtra(MainActivity.FIELD_REMOTE_NOTIFICATION, notification)
    }

    /**
     * Builds and displays a local notification.
     *
     * @param localNotificationId The unique ID for this local notification, used for PendingIntent
     *                            request codes and as the Android notification ID.
     * @param notification The notification content to display.
     * @param notificationTappedIntent The intent to fire when the notification is tapped.
     * @param actions Optional action buttons to add to the notification.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun buildAndDisplayLocalNotification(
        localNotificationId: Int,
        notification: Notification,
        notificationTappedIntent: Intent,
        actions: List<Pair<String, Intent>> = emptyList()
    ) {
        val channelType = notification.channelType
        getNotificationBuilder(notification).apply {
            val notificationContentIntent =
                buildPendingIntentForGivenIntent(localNotificationId, notificationTappedIntent)
            setContentIntent(notificationContentIntent)
            actions.forEach { action ->
                addAction(
                    R.drawable.ic_woo_w_notification,
                    action.first,
                    buildPendingIntentForGivenIntent(localNotificationId, action.second)
                )
            }
            setLargeIcon(getLargeIconBitmap(context, notification.icon, channelType.shouldCircularizeNoteIcon()))
            // Call processing service when notification is dismissed
            val pendingDeleteIntent = NotificationsProcessingService.getPendingIntentForLocalNotificationDismiss(
                context,
                localNotificationId,
                notification.tag!!
            )
            setDeleteIntent(pendingDeleteIntent)
            NotificationManagerCompat.from(context).notify(
                notification.tag,
                localNotificationId,
                build()
            )
        }
    }

    fun buildAndDisplayWooNotification(
        pushId: Int,
        notification: Notification,
        source: NotificationSource,
        analyticsId: String?,
        isGroupNotification: Boolean
    ) {
        val channelType = notification.channelType
        getNotificationBuilder(notification).apply {
            setLargeIcon(getLargeIconBitmap(context, notification.icon, channelType.shouldCircularizeNoteIcon()))
            setDefaults(NotificationCompat.DEFAULT_ALL)
        }.apply {
            showNotification(pushId, notification, source, analyticsId, this)

            // Also add a group summary notification, which is required for non-wearable devices
            // Do not need to play the sound again. We've already played it in the individual builder.
            if (!isGroupNotification) {
                setGroupSummary(true)
                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                setDefaults(0)
                setSound(null)
                setVibrate(null)
                showNotification(notification.getGroupPushId(), notification, source, analyticsId, this)
            }
        }
    }

    fun buildAndDisplayWooGroupNotification(
        inboxMessage: String,
        subject: String,
        notification: Notification,
        source: NotificationSource,
        analyticsId: String?
    ) {
        val inboxStyle = NotificationCompat.InboxStyle().addLine(inboxMessage)
        val channelId = with(notificationChannelsHandler) { notification.channelType.getChannelId() }

        NotificationCompat.Builder(context, channelId)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSmallIcon(R.drawable.ic_woo_w_notification)
            .setColor(ContextCompat.getColor(context, R.color.color_primary))
            .setGroup(notification.getGroup())
            .setGroupSummary(true)
            .setTicker(notification.noteMessage)
            .setContentTitle(notification.noteTitle)
            .setContentText(subject)
            .setStyle(inboxStyle)
            .setSound(null)
            .setVibrate(null)
            .apply {
                showNotification(
                    notification.getGroupPushId(),
                    notification,
                    source,
                    analyticsId,
                    this
                )
            }
    }

    private fun showNotification(
        pushId: Int,
        notification: Notification,
        source: NotificationSource,
        analyticsId: String?,
        builder: NotificationCompat.Builder
    ) {
        try {
            builder.addExtras(
                Bundle().apply {
                    putLong(EXTRA_REMOTE_NOTE_ID, notification.remoteNoteId)
                    putLong(EXTRA_REMOTE_SITE_ID, notification.remoteSiteId)
                    putString(EXTRA_CHANNEL_TYPE, notification.channelType.name)
                    putString(EXTRA_NOTE_MESSAGE, notification.noteMessage)
                    putString(EXTRA_NOTE_TYPE, notification.noteType.trackingValue)
                    putString(EXTRA_SOURCE, source.name)
                    putString(EXTRA_ANALYTICS_ID, analyticsId)
                }
            )

            val pendingDeleteIntent = NotificationsProcessingService.getPendingIntentForPushNotificationDismiss(
                context,
                pushId
            )
            builder.setDeleteIntent(pendingDeleteIntent)

            val flags = if (SystemVersionUtils.isAtLeastS()) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                pushId,
                getResultIntent(pushId, notification),
                flags
            )
            builder.setContentIntent(pendingIntent)
            @SuppressLint("MissingPermission") // Lint won't detect we are checking the permission right below
            if (isNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(pushId, builder.build())
            }
        } catch (e: RemoteException) {
            // see https://github.com/woocommerce/woocommerce-android/issues/920
            WooLog.e(WooLog.T.NOTIFICATIONS, e)
        }
    }

    private fun buildPendingIntentForGivenIntent(notificationLocalId: Int, intent: Intent): PendingIntent {
        val flags = if (SystemVersionUtils.isAtLeastS()) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, notificationLocalId, intent, flags)
    }

    private fun getLargeIconBitmap(
        context: Context,
        iconUrl: String?,
        shouldCircularizeIcon: Boolean
    ): Bitmap? {
        return iconUrl?.let {
            try {
                val decodedIconUrl = URLDecoder.decode(iconUrl, "UTF-8")
                val largeIconSize = context.resources.getDimensionPixelSize(
                    android.R.dimen.notification_large_icon_height
                )
                val largeIconBitmap = Glide.with(context)
                    .asBitmap()
                    .loadPhotonUrlWithFallback(decodedIconUrl, largeIconSize, largeIconSize)
                    .submit()
                    .get()

                if (largeIconBitmap != null && shouldCircularizeIcon) {
                    ImageUtils.getCircularBitmap(largeIconBitmap)
                }

                largeIconBitmap
            } catch (e: UnsupportedEncodingException) {
                WooLog.e(WooLog.T.NOTIFICATIONS, e)
                null
            } catch (e: ExecutionException) {
                // ExecutionException happens when the image fails to load.
                // handling the exception here to gracefully display notification, without icon
                // instead of crashing the app
                WooLog.e(WooLog.T.NOTIFICATIONS, "Failed to load image with url $iconUrl")
                null
            }
        }
    }

    companion object {
        private const val EXTRA_REMOTE_NOTE_ID = "remote_note_id"
        private const val EXTRA_REMOTE_SITE_ID = "remote_site_id"
        private const val EXTRA_CHANNEL_TYPE = "channel_type"
        private const val EXTRA_NOTE_MESSAGE = "note_message"
        private const val EXTRA_NOTE_TYPE = "note_type"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_ANALYTICS_ID = "analytics_id"
    }
}

data class ActiveNotificationData(
    val id: Int,
    val remoteNoteId: Long,
    val remoteSiteId: Long,
    val channelType: String?,
    val noteMessage: String?,
    val noteTypeTrackingValue: String?,
    val source: NotificationSource?,
    val analyticsId: String?,
    val isGroupSummary: Boolean = false
)
