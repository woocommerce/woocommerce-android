package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.AppPrefsWrapper
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

    fun onLoginWithWordPressAccount() {
        cancelCurrentNotificationWorkRequest()
        val notificationData = workDataOf(
            LOGIN_NOTIFICATION_TYPE_KEY to LoginSupportNotificationType.DEFAULT_SUPPORT.name
        )
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInputData(notificationData)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

        prefsWrapper.setLocalNotificationWorkRequestId(workRequest.stringId)
        workManager.enqueue(workRequest)
    }

    fun cancelCurrentNotificationWorkRequest() {
        val currentWorkRequestId = prefsWrapper.getLocalNotificationWorkRequestId()
        if (currentWorkRequestId.isNotEmpty()) {
            workManager.cancelWorkById(UUID.fromString(currentWorkRequestId))
            prefsWrapper.setLocalNotificationWorkRequestId("")
        }
    }

    enum class LoginSupportNotificationType(val notification: String) {
        NO_LOGIN_INTERACTION("no_login_interaction"),
        LOGIN_ERROR_WRONG_EMAIL("wrong_email"),
        DEFAULT_SUPPORT("default_support")
    }
}
