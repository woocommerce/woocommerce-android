package com.woocommerce.android.notifications.local

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_SITE_ID
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.ui.dashboard.data.ObserveBlazeWidgetStatus
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.util.WooPermissionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.store.SiteStore

@HiltWorker
class PreconditionCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper,
    private val siteStore: SiteStore,
    private val observeBlazeWidgetStatus: ObserveBlazeWidgetStatus,
    private val crashLogging: CrashLogging,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (!canDisplayNotifications) cancelWork("Notifications permission not granted. Cancelling work.")

        val type = LocalNotificationType.fromString(inputData.getString(LOCAL_NOTIFICATION_TYPE))
        val siteId = inputData.getLong(LOCAL_NOTIFICATION_SITE_ID, 0L)
        return when (type) {
            LocalNotificationType.BLAZE_NO_CAMPAIGN_REMINDER -> proceedIfValidSiteAndBlazeAvailable(siteId)
            LocalNotificationType.BLAZE_ABANDONED_CAMPAIGN_REMINDER -> proceedIfValidSiteAndBlazeAvailable(siteId)

            null -> cancelWork("Notification type is null. Cancelling work.")
        }
    }

    private suspend fun proceedIfValidSiteAndBlazeAvailable(siteId: Long) = when {
        siteId == 0L -> {
            val message = "Site id is missing. Cancelling local notification work."
            crashLogging.sendReport(
                exception = Exception(message),
                message = "PreconditionCheckWorker: cancelling work"
            )
            cancelWork(message)
        }

        observeBlazeWidgetStatus().first() != DashboardWidget.Status.Available -> {
            cancelWork("Blaze is not available. Cancelling local notification work.")
        }

        siteStore.getSiteBySiteId(siteId) == null -> {
            cancelWork("The site linked to the notifications doesn't exist in the db. Cancelling work.")
        }

        else -> Result.success()
    }

    private val canDisplayNotifications: Boolean
        get() = VERSION.SDK_INT < VERSION_CODES.TIRAMISU || WooPermissionUtils.hasNotificationsPermission(appContext)

    private fun cancelWork(message: String): Result {
        wooLogWrapper.i(NOTIFICATIONS, message)
        WorkManager.getInstance(appContext).cancelWorkById(id)
        return Result.failure()
    }
}
