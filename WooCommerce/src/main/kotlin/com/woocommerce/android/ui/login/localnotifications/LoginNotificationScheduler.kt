package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.woocommerce.android.AppPrefsWrapper
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
        const val NOTIFICATION_TEST_DELAY_IN_SECONDS = 5L // TODO SET THIS TO 24H BEFORE MERGE
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
                    .setInitialDelay(NOTIFICATION_TEST_DELAY_IN_SECONDS, TimeUnit.SECONDS)
                    .build()

            prefsWrapper.setPreLoginNotificationWorkRequestId(workRequest.id.toString())
            workManager.enqueue(workRequest)
        }
    }

    private fun cancelCurrentNotificationWorkRequest() {
        val currentWorkRequestId = prefsWrapper.getPreLoginNotificationWorkRequestId()
        if (currentWorkRequestId.isNotEmpty()) {
            workManager.cancelWorkById(UUID.fromString(currentWorkRequestId))
            prefsWrapper.setPreLoginNotificationWorkRequestId("")
        }
    }

    enum class LoginHelpNotificationType(private val typeName: String) {
        LOGIN_SITE_ADDRESS_ERROR("site_address_error"),
        DEFAULT_HELP("default_support");

        override fun toString(): String {
            return typeName
        }

        companion object {
            fun fromString(string: String?): LoginHelpNotificationType =
                when (string) {
                    LOGIN_SITE_ADDRESS_ERROR.typeName -> LOGIN_SITE_ADDRESS_ERROR
                    DEFAULT_HELP.typeName -> DEFAULT_HELP
                    else -> DEFAULT_HELP
                }
        }
    }
}
