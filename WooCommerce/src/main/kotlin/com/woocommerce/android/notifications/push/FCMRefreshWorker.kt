package com.woocommerce.android.notifications.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.isGooglePlayServicesAvailable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@HiltWorker
class FCMRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val registerDevice: RegisterDevice,
    private val appPrefs: AppPrefsWrapper
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!context.isGooglePlayServicesAvailable()) return Result.success()

        return runCatching { Firebase.messaging.token.await() }
            .map { token ->
                require(token.isNotNullOrEmpty()) { "Retrieved FCM token is null or empty" }
                token
            }
            .fold(
                onSuccess = { token ->
                    WooLog.d(WooLog.T.NOTIFICATIONS, "FCM token retrieved: $token")
                    appPrefs.setFCMToken(token)
                    registerDevice(RegisterDevice.Mode.FORCEFULLY)
                    Result.success()
                },
                onFailure = { e ->
                    WooLog.e(WooLog.T.NOTIFICATIONS, "Failed to refresh FCM token", e)
                    Result.failure()
                }
            )
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = FCMRefreshWorker::class.java
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val work = PeriodicWorkRequestBuilder<FCMRefreshWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workRequest.simpleName,
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }
    }
}