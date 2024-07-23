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
import com.woocommerce.android.notifications.NotificationChannelType.BACKGROUND_WORKS
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
                checkAndTrackNewOrderNotificationSound(it)
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

    private fun checkAndTrackNewOrderNotificationSound(channel: NotificationChannel) {
        val sound = channel.sound
        var updatedChannel = channel
        if (sound?.toString()?.matches("^android\\.resource.*\\d+$".toRegex()) == true) {
            // The channel still uses the Uri based on the resource id, so we need to recreate it
            WooLog.d(WooLog.T.NOTIFS, "Orders notification channel still uses ID based sound, recreating it.")
            recreateNotificationChannel(NEW_ORDER)
            updatedChannel = notificationManagerCompat.getNotificationChannel(NEW_ORDER.getChannelId())!!
        }

        analyticsTracker.track(
            AnalyticsEvent.NEW_ORDER_PUSH_NOTIFICATION_SOUND,
            mapOf("sound_status" to updatedChannel.getNewOrderNotificationSoundStatus().name)
        )
    }

    fun NotificationChannelType.getChannelId(): String {
        val baseChannelId = context.getString(
            when (this) {
                NEW_ORDER -> R.string.notification_channel_order_id
                REVIEW -> R.string.notification_channel_review_id
                OTHER -> R.string.notification_channel_general_id
                BACKGROUND_WORKS -> R.string.notification_channel_background_works_id
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
                BACKGROUND_WORKS -> R.string.notification_channel_background_works_title
            }
        )
    }

    private fun NotificationChannel.getNewOrderNotificationSoundStatus(): NewOrderNotificationSoundStatus {
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) return NewOrderNotificationSoundStatus.DISABLED
        return when (sound) {
            context.getChaChingUri() -> NewOrderNotificationSoundStatus.DEFAULT
            null -> NewOrderNotificationSoundStatus.DISABLED
            else -> NewOrderNotificationSoundStatus.SOUND_MODIFIED
        }
    }

    enum class NewOrderNotificationSoundStatus {
        DISABLED, DEFAULT, SOUND_MODIFIED
    }
}
