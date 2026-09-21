package com.woocommerce.android.ui.troubleshooting.useCases

import android.Manifest
import android.app.NotificationChannel
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

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun areAppNotificationsEnabled(): Boolean =
        wooNotificationBuilder.isNotificationsEnabled()

    /**
     * The state of every Woo channel, in [NotificationChannelType] declaration order. A channel the merchant
     * silenced still posts, so [ChannelImportance.SILENT] has to be told apart from [ChannelImportance.DEFAULT]:
     * both look identical to any check that only asks whether the channel is off.
     */
    fun wooNotificationChannels(): Map<NotificationChannelType, WooChannelState> =
        NotificationChannelType.entries.associateWith { channelType ->
            val channel = notificationManagerCompat.getNotificationChannel(
                with(notificationChannelsHandler) { channelType.getChannelId() }
            )
            WooChannelState(
                importance = channel.toChannelImportance(),
                canBypassDnd = channel?.canBypassDnd() ?: false
            )
        }

    fun disabledWooNotificationChannels(): List<NotificationChannelType> =
        wooNotificationChannels()
            .filterValues { it.importance == ChannelImportance.OFF }
            .keys
            .toList()

    /**
     * The device-wide interruption filter, which is what both Do Not Disturb and an Android 16 Mode produce. A
     * Mode that filters anything surfaces here as a value other than [DoNotDisturbStatus.OFF]; one whose rule
     * uses `INTERRUPTION_FILTER_ALL` is active without filtering, and reads as [DoNotDisturbStatus.OFF]. Which
     * Mode is active cannot be read at all - `getAutomaticZenRules` needs `ACCESS_NOTIFICATION_POLICY`, which
     * the app does not request, and even then returns only rules the caller owns.
     */
    fun doNotDisturbStatus(): DoNotDisturbStatus =
        when (notificationManagerCompat.currentInterruptionFilter) {
            NotificationManagerCompat.INTERRUPTION_FILTER_ALL -> DoNotDisturbStatus.OFF
            NotificationManagerCompat.INTERRUPTION_FILTER_PRIORITY -> DoNotDisturbStatus.PRIORITY_ONLY
            NotificationManagerCompat.INTERRUPTION_FILTER_ALARMS -> DoNotDisturbStatus.ALARMS_ONLY
            NotificationManagerCompat.INTERRUPTION_FILTER_NONE -> DoNotDisturbStatus.TOTAL_SILENCE
            else -> DoNotDisturbStatus.UNKNOWN
        }

    /**
     * Whether the platform is temporarily hiding this package's notifications, because the package was suspended
     * or marked distracting. Both are system-only calls, so the cause is something with privileged control of the
     * device. This is not Do Not Disturb. Null below Android 10, which has no such call.
     */
    fun areNotificationsPaused(): Boolean? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationManager.areNotificationsPaused()
        } else {
            null
        }

    /**
     * Compared rather than matched constant by constant, because `IMPORTANCE_MAX` is outside the `@Importance`
     * IntDef - it is reserved by the OS, which caps apps at `IMPORTANCE_HIGH` - and `IMPORTANCE_UNSPECIFIED` is
     * negative. Anything below `IMPORTANCE_NONE` is a value we cannot name, which a status report should say.
     */
    private fun NotificationChannel?.toChannelImportance(): ChannelImportance {
        val importance = this?.importance ?: return ChannelImportance.NOT_CREATED
        return when {
            importance < NotificationManager.IMPORTANCE_NONE -> ChannelImportance.UNKNOWN
            importance == NotificationManager.IMPORTANCE_NONE -> ChannelImportance.OFF
            importance < NotificationManager.IMPORTANCE_DEFAULT -> ChannelImportance.SILENT
            importance == NotificationManager.IMPORTANCE_DEFAULT -> ChannelImportance.DEFAULT
            else -> ChannelImportance.HIGH
        }
    }

    data class WooChannelState(
        val importance: ChannelImportance,
        val canBypassDnd: Boolean
    )

    enum class ChannelImportance {
        OFF, SILENT, DEFAULT, HIGH, NOT_CREATED, UNKNOWN
    }

    enum class DoNotDisturbStatus {
        OFF, PRIORITY_ONLY, ALARMS_ONLY, TOTAL_SILENCE, UNKNOWN
    }
}
