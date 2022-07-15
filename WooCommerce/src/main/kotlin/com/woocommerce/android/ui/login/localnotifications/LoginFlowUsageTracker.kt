package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LoginFlowUsageTracker @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        const val LOGIN_NOTIFICATION_TYPE_KEY = "Notification-type"
    }

    fun onLoginWithWordPressAccount() {
        val notificationData = workDataOf(LOGIN_NOTIFICATION_TYPE_KEY to "WordPress")
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<LocalNotificationWorker>()
                .setInputData(notificationData)
//                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueue(uploadWorkRequest)
    }

//    fun cancelScheduledNotification() {
//
//    }
}
