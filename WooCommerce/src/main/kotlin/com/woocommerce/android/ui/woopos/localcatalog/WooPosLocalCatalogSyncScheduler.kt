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
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalCatalogSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: WooPosLogWrapper,
    private val preferencesRepository: WooPosPreferencesRepository,
) {

    private companion object {
        private const val ONE_TIME_WORK_NAME = "PosLocalCatalogSyncOneTime"

        const val REFRESH_INTERVAL_HOURS = 24L
        const val TIME_OF_DAY_FOR_PERIODIC_SYNC = 23 // 11 PM
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    fun schedulePeriodicFullCatalogSync() {
        CoroutineScope(Dispatchers.IO).launch {
            val constraints = getConstraintsBasedOnPreference()
            val syncWorkRequest = PeriodicWorkRequestBuilder<WooPosLocalCatalogSyncWorker>(
                REFRESH_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setInitialDelay(calculateDelayToNight(), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WooPosLocalCatalogSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            )

            logger.d("POS local catalog full sync scheduled.")
        }
    }

    fun triggerManualFullCatalogSync() {
        CoroutineScope(Dispatchers.IO).launch {
            val constraints = getConstraintsBasedOnPreference()
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WooPosLocalCatalogSyncWorker>()
                .setConstraints(constraints)
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
    }

    fun isPeriodicWorkRunning(): Boolean {
        val periodicWork = workManager.getWorkInfosForUniqueWork(WooPosLocalCatalogSyncWorker.WORK_NAME).get()

        return periodicWork.any { it.state == WorkInfo.State.RUNNING }
    }

    fun isOneTimeWorkRunning(): Boolean {
        val oneTimeWork = workManager.getWorkInfosForUniqueWork(ONE_TIME_WORK_NAME).get()

        return oneTimeWork.any { it.state == WorkInfo.State.RUNNING }
    }

    fun updateWorkConstraints() {
        logger.d("Updating work constraints based on cellular preference change")
        cancelPeriodicWork()
        schedulePeriodicFullCatalogSync()
    }

    fun cancelPeriodicWork() {
        workManager.cancelUniqueWork(WooPosLocalCatalogSyncWorker.WORK_NAME)
        logger.d("Cancelled periodic work for constraint update")
    }

    private suspend fun getConstraintsBasedOnPreference(): Constraints {
        val allowCellular = preferencesRepository.allowCellularDataUpdate.first()
        val networkType = if (allowCellular) NetworkType.CONNECTED else NetworkType.UNMETERED

        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
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
