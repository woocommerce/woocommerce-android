package com.woocommerce.android.ui.woopos.localcatalog

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: WooPosLogWrapper,
) {

    private companion object {
        private const val ONE_TIME_WORK_NAME = "PosLocalCatalogSyncOneTime"

        const val REFRESH_INTERVAL_HOURS = 24L
        const val TIME_OF_DAY_FOR_PERIODIC_SYNC = 23 // 11 PM
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun schedulePeriodicFullCatalogSync() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<WooPosLocalCatalogSyncWorker>(
            REFRESH_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setInitialDelay(calculateDelayToNight(), TimeUnit.MILLISECONDS)
            .setConstraints(getConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WooPosLocalCatalogSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        logger.d("POS local catalog full sync scheduled.")
    }

    fun triggerManualFullCatalogSync() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WooPosLocalCatalogSyncWorker>()
            .setConstraints(getConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )

        logger.d("Manual POS local catalog sync triggered")
    }

    fun observePeriodicWorkStatus(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(WooPosLocalCatalogSyncWorker.WORK_NAME)
            .map { workInfos -> workInfos.any { it.state == WorkInfo.State.RUNNING } }
    }

    fun observeOneTimeWorkStatus(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(ONE_TIME_WORK_NAME)
            .map { workInfos -> workInfos.any { it.state == WorkInfo.State.RUNNING } }
    }

    fun observeOneTimeWorkInfo(): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(ONE_TIME_WORK_NAME)
            .map { workInfos -> workInfos.firstOrNull() }
    }

    fun observePeriodicWorkInfo(): Flow<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkFlow(WooPosLocalCatalogSyncWorker.WORK_NAME)
            .map { workInfos -> workInfos.firstOrNull() }
    }

    private fun getConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun calculateDelayToNight(): Long {
        val now = Calendar.getInstance()
        val night = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, TIME_OF_DAY_FOR_PERIODIC_SYNC)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return night.timeInMillis - now.timeInMillis
    }
}
