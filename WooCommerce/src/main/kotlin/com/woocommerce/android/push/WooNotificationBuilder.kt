package com.woocommerce.android.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.woocommerce.android.R
import com.woocommerce.android.model.Notification
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.SystemVersionUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.PhotonUtils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooNotificationBuilder @Inject constructor(private val context: Context) {
    fun isNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }

    fun cancelNotification(pushId: Int) = NotificationManagerCompat.from(context).cancel(pushId)

    fun cancelAllNotifications() = NotificationManagerCompat.from(context).cancelAll()

    private fun getNotificationBuilder(
        channelId: String,
        notification: Notification
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_woo_w_notification)
            .setColor(ContextCompat.getColor(context, R.color.color_primary))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setGroup(notification.getGroup(channelId))
            .setContentTitle(notification.noteTitle)
            .setContentText(notification.noteMessage)
            .setTicker(notification.noteMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.noteMessage))

    private fun getResultIntent(
        pushId: Int,
        notification: Notification
    ) = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.FIELD_OPENED_FROM_PUSH, true)
        putExtra(MainActivity.FIELD_PUSH_ID, pushId)
        if (notification.remoteNoteId != 0L) {
            putExtra(MainActivity.FIELD_REMOTE_NOTIFICATION, notification)
        }
    }

    fun buildAndDisplayZendeskNotification(
        channelId: String,
        notification: Notification
    ) {
        getNotificationBuilder(channelId, notification).apply {
            showNotification(notification.noteId, notification = notification, builder = this)
        }
    }

    fun buildAndDisplayWooNotification(
        pushId: Int,
        defaults: Int,
        channelId: String,
        notification: Notification,
        addCustomNotificationSound: Boolean,
        isGroupNotification: Boolean
    ) {
        val channelType = notification.channelType
        getNotificationBuilder(channelId, notification).apply {
            setLargeIcon(getLargeIconBitmap(context, notification.icon, channelType.shouldCircularizeNoteIcon()))
            setDefaults(defaults)
            if (addCustomNotificationSound) {
                setSound(getChaChingUri(), AudioManager.STREAM_NOTIFICATION)
            }
        }.apply {
            showNotification(pushId, notification, this)

            // Also add a group summary notification, which is required for non-wearable devices
            // Do not need to play the sound again. We've already played it in the individual builder.
            if (!isGroupNotification) {
                setGroupSummary(true)
                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                showNotification(notification.getGroupPushId(), notification, this)
            }
        }
    }

    fun buildAndDisplayWooGroupNotification(
        channelId: String,
        inboxMessage: String,
        subject: String,
        summaryText: String,
        notification: Notification,
        shouldDisplaySummaryText: Boolean,
    ) {
        val inboxStyle = NotificationCompat.InboxStyle().addLine(inboxMessage)

        if (shouldDisplaySummaryText) {
            inboxStyle.setSummaryText(summaryText)
        }
        NotificationCompat.Builder(context, channelId)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setSmallIcon(R.drawable.ic_woo_w_notification)
            .setColor(ContextCompat.getColor(context, R.color.color_primary))
            .setGroup(notification.getGroup(channelId))
            .setGroupSummary(true)
            .setAutoCancel(true)
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
                    this
                )
            }
    }

    fun showNotification(
        pushId: Int,
        notification: Notification,
        builder: NotificationCompat.Builder
    ) {
        try {
            // Call processing service when notification is dismissed
            val pendingDeleteIntent = NotificationsProcessingService.getPendingIntentForNotificationDismiss(
                context, pushId
            )
            builder.setDeleteIntent(pendingDeleteIntent)

            val flags = if (SystemVersionUtils.isAtLeastS()) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context, pushId, getResultIntent(pushId, notification),
                flags
            )
            builder.setContentIntent(pendingIntent)
            NotificationManagerCompat.from(context).notify(pushId, builder.build())
        } catch (e: RemoteException) {
            // see https://github.com/woocommerce/woocommerce-android/issues/920
            WooLog.e(WooLog.T.NOTIFS, e)
        }
    }

    fun createNotificationChannels() {
        for (noteType in NotificationChannelType.values()) {
            createNotificationChannel(
                context.getString(noteType.getChannelId()),
                context.getString(noteType.getChannelTitle()),
                noteType == NotificationChannelType.NEW_ORDER
            )
        }
    }

    /**
     * Ensures the desired notification channel is created when on API 26+, does nothing otherwise since notification
     * channels weren't added until API 26
     */
    private fun createNotificationChannel(channelId: String, channelName: String, addChaChingSound: Boolean) {
        if (SystemVersionUtils.isAtLeastO()) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // check for existing channel first
            manager.getNotificationChannel(channelId)?.let {
                WooLog.i(WooLog.T.NOTIFS, "Notification channel already created with the following attributes: $it")
                return
            }

            // create the channel since it doesn't already exist
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            if (addChaChingSound) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                channel.setSound(getChaChingUri(), attributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun getChaChingUri(): Uri {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.cha_ching)
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
                val resizedUrl = PhotonUtils.getPhotonImageUrl(decodedIconUrl, largeIconSize, largeIconSize)
                val largeIconBitmap = Glide.with(context)
                    .asBitmap()
                    .load(resizedUrl)
                    .submit()
                    .get()

                if (largeIconBitmap != null && shouldCircularizeIcon) {
                    ImageUtils.getCircularBitmap(largeIconBitmap)
                }

                largeIconBitmap
            } catch (e: UnsupportedEncodingException) {
                WooLog.e(WooLog.T.NOTIFS, e)
                null
            } catch (e: ExecutionException) {
                // ExecutionException happens when the image fails to load.
                // handling the exception here to gracefully display notification, without icon
                // instead of crashing the app
                WooLog.e(WooLog.T.NOTIFS, "Failed to load image with url $iconUrl")
                null
            }
        }
    }
}
