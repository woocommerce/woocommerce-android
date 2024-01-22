package com.woocommerce.android.notifications.local

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_SITE_ID
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_EXPIRED
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_EXPIRING
import com.woocommerce.android.notifications.local.LocalNotificationType.FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED
import com.woocommerce.android.notifications.local.LocalNotificationType.SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED
import com.woocommerce.android.notifications.local.LocalNotificationType.STORE_CREATION_FINISHED
import com.woocommerce.android.notifications.local.LocalNotificationType.TEST
import com.woocommerce.android.notifications.local.LocalNotificationType.THREE_DAYS_AFTER_STILL_EXPLORING
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.util.WooPermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.store.SiteStore

@HiltWorker
class PreconditionCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper,
    private val siteStore: SiteStore,
    private val crashLogging: CrashLogging,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        if (!canDisplayNotifications) cancelWork("Notifications permission not granted. Cancelling work.")

        val type = LocalNotificationType.fromString(inputData.getString(LOCAL_NOTIFICATION_TYPE))
        val siteId = inputData.getLong(LOCAL_NOTIFICATION_SITE_ID, 0L)
        return when (type) {
            STORE_CREATION_FINISHED, TEST -> Result.success()
            FREE_TRIAL_EXPIRING,
            FREE_TRIAL_EXPIRED,
            SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED,
            FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED,
            THREE_DAYS_AFTER_STILL_EXPLORING -> proceedIfFreeTrialAndMatchesSite(siteId)

            null -> cancelWork("Notification type is null. Cancelling work.")
        }
    }

    private fun proceedIfFreeTrialAndMatchesSite(siteId: Long): Result {
        if (siteId == 0L) {
            val message = "Site id is missing. Cancelling local notification work."
            crashLogging.sendReport(
                exception = Exception(message),
                message = "PreconditionCheckWorker: cancelling work"
            )
            return cancelWork(message)
        }

        val notificationLinkedSite = siteStore.getSiteBySiteId(siteId)
        return if (notificationLinkedSite.isFreeTrial) {
            Result.success()
        } else {
            if (notificationLinkedSite == null) {
                cancelWork("The site linked to the notifications doesn't exist in the db. Cancelling work.")
            } else cancelWork("Store plan upgraded. Cancelling work.")
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
