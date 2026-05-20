package com.woocommerce.android.ui.troubleshooting.useCases

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.WooNotificationBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationSystemStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wooNotificationBuilder: WooNotificationBuilder,
    private val notificationChannelsHandler: NotificationChannelsHandler
) {
    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun areAppNotificationsEnabled(): Boolean =
        wooNotificationBuilder.isNotificationsEnabled()

    fun disabledWooNotificationChannels(): List<NotificationChannelType> =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            emptyList()
        } else {
            NotificationChannelType.entries.filter { channelType ->
                val channel = notificationManagerCompat.getNotificationChannel(
                    with(notificationChannelsHandler) { channelType.getChannelId() }
                )
                channel?.importance == NotificationManager.IMPORTANCE_NONE
            }
        }
}
