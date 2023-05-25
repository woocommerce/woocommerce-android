package com.woocommerce.android.notifications.local

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_DATA
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.tools.SelectedSite
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
    private val selectedSite: SelectedSite
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val type = inputData.getString(LOCAL_NOTIFICATION_TYPE)
        val data = inputData.getString(LOCAL_NOTIFICATION_DATA)
        return when {
            !canDisplayNotifications -> cancelWork("Notifications permission not granted. Cancelling work.")
            type == null -> cancelWork("Notification check data is invalid")
            type == LocalNotificationType.STORE_CREATION_FINISHED.value -> Result.success()
            type == LocalNotificationType.STORE_CREATION_INCOMPLETE.value -> Result.success()
            type == LocalNotificationType.FREE_TRIAL_EXPIRING.value -> {
                val site = selectedSite.get()
                if (site.isFreeTrial && site.siteId == data?.toLongOrNull()) {
                    Result.success()
                } else {
                    cancelWork("Store plan upgraded or a different site. Cancelling work.")
                }
            }
            type == LocalNotificationType.FREE_TRIAL_EXPIRED.value -> Result.success()
            else -> {
                cancelWork("Unknown notification $type. Cancelling work.")
            }
        }
    }

    private val canDisplayNotifications: Boolean
        get() = VERSION.SDK_INT < VERSION_CODES.TIRAMISU || WooPermissionUtils.hasNotificationsPermission(appContext)

    private fun cancelWork(message: String): Result {
        wooLogWrapper.i(NOTIFICATIONS, message)
        WorkManager.getInstance(appContext).cancelWorkById(id)
        return Result.failure()
    }
}
