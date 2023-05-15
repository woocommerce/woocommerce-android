package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocalNotificationScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        const val LOCAL_NOTIFICATION_TAG = "local-notification-tag"
        const val LOCAL_NOTIFICATION_ID = "local-notification-id"
        const val LOCAL_NOTIFICATION_TITLE = "local-notification-title"
        const val LOCAL_NOTIFICATION_DESC = "local-notification-description"
    }

    private val workManager = WorkManager.getInstance(appContext)

    fun scheduleNotification(notification: LocalNotification) {
        cancelScheduledNotification(notification)
        val notificationData = workDataOf(
            LOCAL_NOTIFICATION_TAG to notification.tag,
            LOCAL_NOTIFICATION_ID to notification.id,
            LOCAL_NOTIFICATION_TITLE to notification.title,
            LOCAL_NOTIFICATION_DESC to notification.description
        )
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInputData(notificationData)
                .addTag(notification.tag)
                .setInitialDelay(notification.delay, notification.delayUnit)
                .build()

        AnalyticsTracker.track(
            AnalyticsEvent.LOCAL_NOTIFICATION_SCHEDULED,
            mapOf(AnalyticsTracker.KEY_TYPE to notification.tag)
        )
        workManager.enqueue(workRequest)
    }

    fun onNotificationTapped(notification: LocalNotification) {
        NotificationManagerCompat.from(appContext).cancel(
            notification.tag,
            notification.id
        )
        AnalyticsTracker.track(
            AnalyticsEvent.LOCAL_NOTIFICATION_TAPPED,
            mapOf(AnalyticsTracker.KEY_TYPE to notification.tag)
        )
    }

    private fun cancelScheduledNotification(notification: LocalNotification) {
        workManager.cancelAllWorkByTag(notification.tag)
    }
}
