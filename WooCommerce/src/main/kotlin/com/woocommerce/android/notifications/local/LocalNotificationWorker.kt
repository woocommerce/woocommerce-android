package com.woocommerce.android.notifications.local

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.LOCAL_NOTIFICATION_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Notification
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType.REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_DESC
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_ID
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TAG
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TITLE
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("UnusedPrivateMember")
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooNotificationBuilder: WooNotificationBuilder,
    private val wooLogWrapper: WooLogWrapper
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val tag = inputData.getString(LOCAL_NOTIFICATION_TAG)
        val id = inputData.getInt(LOCAL_NOTIFICATION_ID, -1)
        val title = inputData.getString(LOCAL_NOTIFICATION_TITLE)
        val description = inputData.getString(LOCAL_NOTIFICATION_DESC)

        if (tag != null && id != -1 && title != null && description != null) {
            wooNotificationBuilder.buildAndDisplayLocalNotification(
                tag,
                id,
                appContext.getString(R.string.notification_channel_general_id),
                buildNotification(id, title, description),
                getIntent(appContext, buildNotification(id, title, description)),
            )

            AnalyticsTracker.track(
                LOCAL_NOTIFICATION_DISPLAYED,
                mapOf(AnalyticsTracker.KEY_TYPE to tag)
            )
        } else {
            wooLogWrapper.e(T.NOTIFICATIONS, "Scheduled local notification data is invalid")
        }
        return Result.success()
    }

    private fun getIntent(context: Context, notification: Notification): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.FIELD_LOCAL_NOTIFICATION, notification)
        }
    }

    private fun buildNotification(id: Int, title: String, description: String) = Notification(
        noteId = id,
        uniqueId = 0,
        remoteNoteId = 0,
        remoteSiteId = 0,
        icon = null,
        noteTitle = title,
        noteMessage = description,
        noteType = REMINDER,
        channelType = OTHER
    )
}
