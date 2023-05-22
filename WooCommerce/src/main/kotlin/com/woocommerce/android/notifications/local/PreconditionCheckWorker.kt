package com.woocommerce.android.notifications.local

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
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
import com.woocommerce.android.util.WooPermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PreconditionCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper,
    private val appPrefs: AppPrefsWrapper
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        return if (canDisplayNotifications) {
            val type = inputData.getString(LOCAL_NOTIFICATION_TYPE)
            if (type != null) {
                when (type) {
                    STORE_CREATION_COMPLETE_NOTICE -> {
                        if (appPrefs.wasStoreOpened) {
                            cancelWork("Store already opened, skipping notification $type")
                        } else {
                            Result.success()
                        }
                    }
                    FREE_TRIAL_REMINDER,
                    FREE_TRIAL_EXPIRING_NOTICE,
                    FREE_TRIAL_EXPIRED_NOTICE -> {
                        Result.success()
                    }
                    else -> {
                        cancelWork("Unknown notification $type. Cancelling work.")
                    }
                }
            } else {
                cancelWork("Notification check data is invalid")
            }
        } else {
            cancelWork("Notifications permission not granted. Cancelling work.")
        }
    }

    private val canDisplayNotifications: Boolean
        get() = VERSION.SDK_INT < VERSION_CODES.TIRAMISU || WooPermissionUtils.hasNotificationsPermission(
            appContext
        )

    private fun cancelWork(message: String): Result {
        wooLogWrapper.i(NOTIFICATIONS, message)
        WorkManager.getInstance(appContext).cancelWorkById(id)
        return Result.failure()
    }
}
