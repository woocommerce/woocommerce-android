package com.woocommerce.android.notifications.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.notifications.local.LocalNotification.Companion.NOTIFICATION_TYPE_AFTER_FREE_TRIAL
import com.woocommerce.android.notifications.local.LocalNotification.Companion.NOTIFICATION_TYPE_BEFORE_FREE_TRIAL
import com.woocommerce.android.notifications.local.LocalNotification.Companion.NOTIFICATION_TYPE_STORE_CREATION_COMPLETE
import com.woocommerce.android.notifications.local.LocalNotification.Companion.NOTIFICATION_TYPE_WITHOUT_FREE_TRIAL
import com.woocommerce.android.notifications.local.LocalNotificationScheduler.Companion.LOCAL_NOTIFICATION_TYPE
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.WooLogWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("ComplexCondition", "ForbiddenComment")
class ConditionCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooLogWrapper: WooLogWrapper,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val tag = inputData.getString(LOCAL_NOTIFICATION_TYPE)
        return if (tag != null) {
            when (tag) {
                "test", // TODO: Remove
                NOTIFICATION_TYPE_STORE_CREATION_COMPLETE,
                NOTIFICATION_TYPE_WITHOUT_FREE_TRIAL,
                NOTIFICATION_TYPE_BEFORE_FREE_TRIAL,
                NOTIFICATION_TYPE_AFTER_FREE_TRIAL -> {
                    wooLogWrapper.i(NOTIFICATIONS, "Condition satisfied for $tag")
                    return Result.success()
                }
                else -> {
                    wooLogWrapper.i(NOTIFICATIONS, "Condition NOT satisfied for $tag. Cancelling work.")
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
