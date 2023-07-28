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
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_EXPIRED
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_EXPIRING
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED
import com.woocommerce.android.notifications.local.LocalNotificationType.SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED
import com.woocommerce.android.notifications.local.LocalNotificationType.STORE_CREATION_FINISHED
import com.woocommerce.android.notifications.local.LocalNotificationType.STORE_CREATION_INCOMPLETE
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
        if (!canDisplayNotifications) cancelWork("Notifications permission not granted. Cancelling work.")

        val type = LocalNotificationType.fromString(inputData.getString(LOCAL_NOTIFICATION_TYPE))
        val data = inputData.getString(LOCAL_NOTIFICATION_DATA)
        return when (type) {
            STORE_CREATION_FINISHED,
            STORE_CREATION_INCOMPLETE -> Result.success()

            FREE_TRIAL_EXPIRING,
            FREE_TRIAL_EXPIRED,
            SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED,
            FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED -> proceedIfFreeTrialAndMatchesSite(data?.toLongOrNull())

            null -> cancelWork("Notification type is null. Cancelling work.")
        }
    }

    private fun proceedIfFreeTrialAndMatchesSite(siteId: Long?): Result {
        val site = selectedSite.get()
        return if (site.isFreeTrial && site.siteId == siteId) {
            Result.success()
        } else {
            cancelWork("Store plan upgraded or a different site. Cancelling work.")
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
