package com.woocommerce.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.NotificationChannelType.REVIEW
import com.woocommerce.android.util.WooLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelsHandler @Inject constructor(
    private val context: Context,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun init() {
        createChannels()
    }

    fun recreateNotificationChannel(channelType: NotificationChannelType) {
        notificationManagerCompat.deleteNotificationChannel(channelType.getChannelId())
        appPrefsWrapper.incrementNotificationChannelTypeSuffix(channelType)
        createChannel(channelType)
    }

    fun checkNewOrderNotificationSound(): NewOrderNotificationSoundStatus {
        val channel = notificationManagerCompat.getNotificationChannel(NEW_ORDER.getChannelId())
            // If the notification is not created yet, then it'll have the correct sound when created
            ?: return NewOrderNotificationSoundStatus.DEFAULT

        return channel.getNewOrderNotificationSoundStatus()
    }

    private fun createChannels() {
        NotificationChannelType.entries.forEach {
            createChannel(it)
        }
    }

    private fun createChannel(channelType: NotificationChannelType) {
        // check for existing channel first
        notificationManagerCompat.getNotificationChannel(channelType.getChannelId())?.let {
            if (channelType == NEW_ORDER) {
                analyticsTracker.track(
                    AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_SOUND,
                    mapOf("sound_status" to it.getNewOrderNotificationSoundStatus().name)
                )
            }
            WooLog.i(WooLog.T.NOTIFS, "Notification channel already created with the following attributes: $it")
            return
        }

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

    private fun NotificationChannel.getNewOrderNotificationSoundStatus(): NewOrderNotificationSoundStatus {
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) return NewOrderNotificationSoundStatus.DISABLED
        return when {
            sound.toString().contains(context.packageName) -> NewOrderNotificationSoundStatus.DEFAULT
            sound == null -> NewOrderNotificationSoundStatus.DISABLED
            else -> NewOrderNotificationSoundStatus.SOUND_MODIFIED
        }
    }

    enum class NewOrderNotificationSoundStatus {
        DISABLED, DEFAULT, SOUND_MODIFIED
    }
}
