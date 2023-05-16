package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
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
            LOCAL_NOTIFICATION_TAG to notification.type,
            LOCAL_NOTIFICATION_ID to notification.id,
            LOCAL_NOTIFICATION_TITLE to notification.title,
            LOCAL_NOTIFICATION_DESC to notification.description
        )
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInputData(notificationData)
                .addTag(notification.type)
                .setInitialDelay(notification.delay, notification.delayUnit)
                .build()

        AnalyticsTracker.track(
            AnalyticsEvent.LOCAL_NOTIFICATION_SCHEDULED,
            mapOf(AnalyticsTracker.KEY_TYPE to notification.type)
        )

        WooLog.d(T.NOTIFICATIONS, "Local notification SCHEDULED: $notification")

        workManager.enqueue(workRequest)
    }

    private fun cancelScheduledNotification(notification: LocalNotification) {
        workManager.cancelAllWorkByTag(notification.type)
    }
}
