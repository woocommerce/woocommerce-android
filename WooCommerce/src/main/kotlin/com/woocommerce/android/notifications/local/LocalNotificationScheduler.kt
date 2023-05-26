package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocalNotificationScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val resourceProvider: ResourceProvider
) {
    companion object {
        const val LOCAL_NOTIFICATION_WORK_NAME = "Scheduled notification work"
        const val LOCAL_NOTIFICATION_TYPE = "local_notification_type"
        const val LOCAL_NOTIFICATION_ID = "local_notification_id"
        const val LOCAL_NOTIFICATION_TITLE = "local_notification_title"
        const val LOCAL_NOTIFICATION_DESC = "local_notification_description"
        const val LOCAL_NOTIFICATION_DATA = "local_notification_data"
    }

    private val workManager = WorkManager.getInstance(appContext)

    fun scheduleNotification(notification: LocalNotification) {
        cancelScheduledNotification(notification.type)

        workManager
            .beginUniqueWork(LOCAL_NOTIFICATION_WORK_NAME, REPLACE, buildPreconditionCheckWorkRequest(notification))
            .then(buildNotificationWorkRequest(notification))
            .enqueue()

        AnalyticsTracker.track(
            AnalyticsEvent.LOCAL_NOTIFICATION_SCHEDULED,
            mapOf(AnalyticsTracker.KEY_TYPE to notification.type.value)
        )
    }

    private fun buildPreconditionCheckWorkRequest(notification: LocalNotification): OneTimeWorkRequest {
        val conditionData = workDataOf(
            LOCAL_NOTIFICATION_TYPE to notification.type.value,
            LOCAL_NOTIFICATION_DATA to notification.data
        )
        return OneTimeWorkRequestBuilder<PreconditionCheckWorker>()
            .setInputData(conditionData)
            .addTag(notification.type.value)
            .setInitialDelay(notification.delay, notification.delayUnit)
            .build()
    }

    private fun buildNotificationWorkRequest(notification: LocalNotification): OneTimeWorkRequest {
        val notificationData = workDataOf(
            LOCAL_NOTIFICATION_TYPE to notification.type.value,
            LOCAL_NOTIFICATION_ID to notification.id,
            LOCAL_NOTIFICATION_TITLE to notification.getTitleString(resourceProvider),
            LOCAL_NOTIFICATION_DESC to notification.getDescriptionString(resourceProvider),
            LOCAL_NOTIFICATION_DATA to notification.data
        )
        return OneTimeWorkRequestBuilder<LocalNotificationWorker>()
            .addTag(notification.type.value)
            .setInputData(notificationData)
            .build()
    }

    fun cancelScheduledNotification(type: LocalNotificationType) {
        workManager.cancelAllWorkByTag(type.value)
    }
}
