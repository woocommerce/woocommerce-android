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
        const val LOCAL_NOTIFICATION_SITE_ID = "local_notification_site_id"
    }

    private val workManager = WorkManager.getInstance(appContext)

    fun scheduleNotification(notification: LocalNotification) {
        cancelScheduledNotification(notification.tag)

        workManager
            .beginUniqueWork(
                LOCAL_NOTIFICATION_WORK_NAME + notification.type.value + notification.siteId,
                REPLACE,
                buildPreconditionCheckWorkRequest(notification)
            )
            .then(buildNotificationWorkRequest(notification))
            .enqueue()

        AnalyticsTracker.track(
            AnalyticsEvent.LOCAL_NOTIFICATION_SCHEDULED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to notification.type.value,
                AnalyticsTracker.KEY_BLOG_ID to notification.siteId,
            )
        )
    }

    private fun buildPreconditionCheckWorkRequest(notification: LocalNotification): OneTimeWorkRequest {
        val conditionData = workDataOf(
            LOCAL_NOTIFICATION_TYPE to notification.type.value,
            LOCAL_NOTIFICATION_DATA to notification.data,
            LOCAL_NOTIFICATION_SITE_ID to notification.siteId
        )
        return OneTimeWorkRequestBuilder<PreconditionCheckWorker>()
            .setInputData(conditionData)
            .addTag(notification.tag)
            .setInitialDelay(notification.delay, notification.delayUnit)
            .build()
    }

    private fun buildNotificationWorkRequest(notification: LocalNotification): OneTimeWorkRequest {
        val notificationData = workDataOf(
            LOCAL_NOTIFICATION_TYPE to notification.type.value,
            LOCAL_NOTIFICATION_ID to notification.id,
            LOCAL_NOTIFICATION_TITLE to notification.getTitleString(resourceProvider),
            LOCAL_NOTIFICATION_DESC to notification.getDescriptionString(resourceProvider),
            LOCAL_NOTIFICATION_DATA to notification.data,
            LOCAL_NOTIFICATION_SITE_ID to notification.siteId
        )
        return OneTimeWorkRequestBuilder<LocalNotificationWorker>()
            .addTag(notification.tag)
            .setInputData(notificationData)
            .build()
    }

    private fun cancelScheduledNotification(tag: String) {
        workManager.cancelAllWorkByTag(tag)
    }
}
