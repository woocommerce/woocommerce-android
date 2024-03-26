package com.woocommerce.android.notifications.local

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.analytics.AnalyticsEvent.LOCAL_NOTIFICATION_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Notification
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType.LOCAL_REMINDER
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_DATA
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_DESC
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_ID
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_SITE_ID
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TITLE
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("ComplexCondition")
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooNotificationBuilder: WooNotificationBuilder,
    private val wooLogWrapper: WooLogWrapper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(LOCAL_NOTIFICATION_TYPE)
        val notificationId = inputData.getInt(LOCAL_NOTIFICATION_ID, -1)
        val title = inputData.getString(LOCAL_NOTIFICATION_TITLE)
        val description = inputData.getString(LOCAL_NOTIFICATION_DESC)
        val data = inputData.getString(LOCAL_NOTIFICATION_DATA)
        val siteId = inputData.getLong(LOCAL_NOTIFICATION_SITE_ID, 0L)

        if (siteId != 0L && type != null && notificationId != -1 && title != null && description != null) {
            val notification = buildNotification(notificationId, siteId, type, title, description, data)
            wooNotificationBuilder.buildAndDisplayLocalNotification(
                notification = notification,
                notificationTappedIntent = getIntent(notification),
            )

            AnalyticsTracker.track(
                LOCAL_NOTIFICATION_DISPLAYED,
                mapOf(
                    AnalyticsTracker.KEY_TYPE to type,
                    AnalyticsTracker.KEY_BLOG_ID to siteId,
                )
            )
        } else {
            wooLogWrapper.e(T.NOTIFICATIONS, "Scheduled local notification data is invalid")
        }
        return Result.success()
    }

    private fun getIntent(notification: Notification): Intent {
        return Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.FIELD_LOCAL_NOTIFICATION, notification)
        }
    }

    @Suppress("LongParameterList")
    private fun buildNotification(
        id: Int,
        siteId: Long,
        type: String,
        title: String,
        description: String,
        data: String?
    ) = Notification(
        noteId = id,
        tag = type,
        uniqueId = 0,
        remoteNoteId = 0,
        remoteSiteId = siteId,
        icon = null,
        noteTitle = title,
        noteMessage = description,
        noteType = LOCAL_REMINDER,
        channelType = OTHER,
        data = data
    )
}
