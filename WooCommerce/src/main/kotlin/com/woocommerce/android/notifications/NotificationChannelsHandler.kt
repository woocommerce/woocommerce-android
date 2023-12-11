package com.woocommerce.android.notifications

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.notifications.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.NotificationChannelType.REVIEW
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelsHandler @Inject constructor(
    private val context: Context,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    init {
        createChannels()
    }

    fun recreateNotificationChannel(channelType: NotificationChannelType) {
        notificationManagerCompat.deleteNotificationChannel(channelType.getChannelId())
        appPrefsWrapper.incrementNotificationChannelTypeSuffix(channelType)
        createChannel(channelType)
    }

    fun checkNotificationChannelSound(channelType: NotificationChannelType): Boolean {
        val channel = notificationManagerCompat.getNotificationChannel(channelType.getChannelId())
            ?: return true // If the notification is not created yet, then it'll have the correct sound when created

        return channel.importance >= NotificationManager.IMPORTANCE_DEFAULT && channel.sound != null
    }

    private fun createChannels() {
        NotificationChannelType.values().forEach {
            createChannel(it)
        }
    }

    private fun createChannel(channelType: NotificationChannelType) {
        val channel = NotificationChannelCompat.Builder(
            channelType.getChannelId(),
            NotificationManager.IMPORTANCE_DEFAULT
        )
            .setName(channelType.getChannelTitle())
            .apply {
                if (channelType == NEW_ORDER) {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(context.getChaChingUri(), attributes)
                }
            }
            .build()

        notificationManagerCompat.createNotificationChannel(channel)
    }

    fun NotificationChannelType.getChannelId(): String {
        val baseChannelId = context.getString(
            when (this) {
                NEW_ORDER -> R.string.notification_channel_order_id
                REVIEW -> R.string.notification_channel_review_id
                OTHER -> R.string.notification_channel_general_id
            }
        )
        val suffix = appPrefsWrapper.getNotificationChannelTypeSuffix(this)

        return suffix?.let { "$baseChannelId-$it" } ?: baseChannelId
    }

    private fun NotificationChannelType.getChannelTitle(): String {
        return context.getString(
            when (this) {
                NEW_ORDER -> R.string.notification_channel_order_title
                REVIEW -> R.string.notification_channel_review_title
                OTHER -> R.string.notification_channel_general_title
            }
        )
    }
}
