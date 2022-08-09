package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LoginNotificationScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefsWrapper: AppPrefsWrapper
) {
    companion object {
        const val LOGIN_NOTIFICATION_TYPE_KEY = "Notification-type"
        const val NOTIFICATION_TEST_DELAY_IN_HOURS = 24L
        const val LOGIN_HELP_NOTIFICATION_ID = 987612345
        const val LOGIN_HELP_NOTIFICATION_TAG = "login-help-notification"
    }

    private val workManager = WorkManager.getInstance(appContext)

    fun onLoginSuccess() {
        cancelCurrentNotificationWorkRequest()
        NotificationManagerCompat.from(appContext).cancel(
            LOGIN_HELP_NOTIFICATION_TAG,
            LOGIN_HELP_NOTIFICATION_ID
        )
    }

    fun scheduleNotification(notificationType: LoginHelpNotificationType) {
        if (!prefsWrapper.hasPreLoginNotificationBeenDisplayed()) {
            cancelCurrentNotificationWorkRequest()
            val notificationData = workDataOf(
                LOGIN_NOTIFICATION_TYPE_KEY to notificationType.toString()
            )
            val workRequest: WorkRequest =
                OneTimeWorkRequestBuilder<LoginHelpNotificationWorker>()
                    .setInputData(notificationData)
                    .setInitialDelay(NOTIFICATION_TEST_DELAY_IN_HOURS, TimeUnit.HOURS)
                    .build()

            prefsWrapper.setPreLoginNotificationWorkRequestId(workRequest.id.toString())
            AnalyticsTracker.track(
                AnalyticsEvent.LOGIN_LOCAL_NOTIFICATION_SCHEDULED,
                mapOf(AnalyticsTracker.KEY_TYPE to notificationType.toString())
            )
            workManager.enqueue(workRequest)
        }
    }

    fun onPasswordLoginError() {
        val notificationType = when {
            !prefsWrapper.getLoginSiteAddress()
                .isNullOrBlank() -> LoginHelpNotificationType.LOGIN_SITE_ADDRESS_EMAIL_ERROR
            else -> LoginHelpNotificationType.LOGIN_WPCOM_EMAIL_ERROR
        }
        scheduleNotification(notificationType)
    }

    fun onNotificationTapped(loginHelpNotification: String?) {
        NotificationManagerCompat.from(appContext).cancel(
            LOGIN_HELP_NOTIFICATION_TAG,
            LOGIN_HELP_NOTIFICATION_ID
        )
        AnalyticsTracker.track(
            AnalyticsEvent.LOGIN_LOCAL_NOTIFICATION_TAPPED,
            mapOf(AnalyticsTracker.KEY_TYPE to loginHelpNotification)
        )
    }

    private fun cancelCurrentNotificationWorkRequest() {
        val currentWorkRequestId = prefsWrapper.getPreLoginNotificationWorkRequestId()
        if (currentWorkRequestId.isNotEmpty()) {
            workManager.cancelWorkById(UUID.fromString(currentWorkRequestId))
            prefsWrapper.setPreLoginNotificationWorkRequestId("")
        }
    }
}
