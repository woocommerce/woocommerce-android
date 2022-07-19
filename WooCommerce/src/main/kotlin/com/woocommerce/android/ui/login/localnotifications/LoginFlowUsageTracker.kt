package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.login.localnotifications.LocalNotificationWorker.Companion.PRE_LOGIN_LOCAL_NOTIFICATION_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LoginFlowUsageTracker @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefsWrapper: AppPrefsWrapper
) {
    companion object {
        const val LOGIN_NOTIFICATION_TYPE_KEY = "Notification-type"
    }

    private val workManager = WorkManager.getInstance(appContext)

    fun onLoginSuccess() {
        cancelCurrentNotificationWorkRequest()
        NotificationManagerCompat.from(appContext).cancel(PRE_LOGIN_LOCAL_NOTIFICATION_ID)
    }

    fun scheduleNotification(notificationType: LoginSupportNotificationType) {
        cancelCurrentNotificationWorkRequest()
        val notificationData = workDataOf(
            LOGIN_NOTIFICATION_TYPE_KEY to notificationType.name
        )
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInputData(notificationData)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

        prefsWrapper.setLocalNotificationWorkRequestId(workRequest.stringId)
        workManager.enqueue(workRequest)
    }

    private fun cancelCurrentNotificationWorkRequest() {
        val currentWorkRequestId = prefsWrapper.getLocalNotificationWorkRequestId()
        if (currentWorkRequestId.isNotEmpty()) {
            workManager.cancelWorkById(UUID.fromString(currentWorkRequestId))
            prefsWrapper.setLocalNotificationWorkRequestId("")
        }
    }

    enum class LoginSupportNotificationType(val notification: String) {
        NO_LOGIN_INTERACTION("no_login_interaction"),
        LOGIN_ERROR_WRONG_EMAIL("wrong_email"),
        LOGIN_SITE_ADDRESS_ERROR("site_address_error"),
        DEFAULT_SUPPORT("default_support")
    }
}
