package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification.Companion.FREE_TRIAL_EXPIRED_NOTICE
import com.woocommerce.android.notifications.local.LocalNotification.Companion.FREE_TRIAL_EXPIRING_NOTICE
import com.woocommerce.android.notifications.local.LocalNotification.Companion.FREE_TRIAL_REMINDER
import com.woocommerce.android.notifications.local.LocalNotification.Companion.STORE_CREATION_COMPLETE_NOTICE
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.WooLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InitializationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper,
    private val appPrefs: AppPrefsWrapper
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val type = inputData.getString(LOCAL_NOTIFICATION_TYPE)
        return if (type != null) {
            when (type) {
                STORE_CREATION_COMPLETE_NOTICE -> {
                    appPrefs.wasStoreOpened = false
                    return Result.success()
                }
                FREE_TRIAL_REMINDER,
                FREE_TRIAL_EXPIRING_NOTICE,
                FREE_TRIAL_EXPIRED_NOTICE -> {
                    return Result.success()
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
