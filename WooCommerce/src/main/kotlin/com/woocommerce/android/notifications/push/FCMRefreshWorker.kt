package com.woocommerce.android.notifications.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.isGooglePlayServicesAvailable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.TimeUnit

@HiltWorker
class FCMRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val registerDevice: RegisterDevice,
    private val identityStore: WooPushIdentityStore
) : CoroutineWorker(context, params) {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        if (!context.isGooglePlayServicesAvailable()) return Result.success()

        WooLog.d(WooLog.T.NOTIFICATIONS, "Refreshing FCM token")
        return try {
            identityStore.prepareRegistration(forceRefresh = true)
            registerDevice(RegisterDevice.Trigger.TOKEN_REFRESH)
            Result.success()
        } catch (throwable: Throwable) {
            currentCoroutineContext().ensureActive()
            WooLog.e(WooLog.T.NOTIFICATIONS, "Failed to refresh FCM token", throwable)
            Result.retry()
        }
    }

    companion object {
        @Suppress("MagicNumber")
        fun schedule(context: Context) {
            val workRequest = FCMRefreshWorker::class.java
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val work = PeriodicWorkRequestBuilder<FCMRefreshWorker>(Duration.ofDays(7))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workRequest.simpleName,
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }
    }
}
