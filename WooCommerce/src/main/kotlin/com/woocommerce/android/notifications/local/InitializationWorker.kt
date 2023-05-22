package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.WooLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InitializationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val type = inputData.getString(LOCAL_NOTIFICATION_TYPE)
        return if (type != null) {
            when (type) {
                LocalNotificationType.STORE_CREATION_FINISHED.value,
                LocalNotificationType.STORE_CREATION_INCOMPLETE.value,
                LocalNotificationType.FREE_TRIAL_EXPIRING.value,
                LocalNotificationType.FREE_TRIAL_EXPIRED.value -> {
                    Result.success()
                }
                else -> {
                    wooLogWrapper.i(NOTIFICATIONS, "Unknown notification $type. Cancelling work.")
                    WorkManager.getInstance(appContext).cancelWorkById(id)
                    Result.failure()
                }
            }
        } else {
            wooLogWrapper.e(NOTIFICATIONS, "Notification check data is invalid")
            Result.failure()
        }
    }
}
