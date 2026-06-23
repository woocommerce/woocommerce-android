package com.woocommerce.android.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.StoreConnectionErrorMonitor
import com.woocommerce.android.ui.login.AccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("LongParameterList")
class UpdateDataOnBackgroundWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val updateAnalyticsDashboardRangeSelections: UpdateAnalyticsDashboardRangeSelections,
    private val updateOrderListBySelectedStore: UpdateOrderListBySelectedStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val storeConnectionErrorMonitor: StoreConnectionErrorMonitor
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val REFRESH_TIME = 4L
        const val WORK_NAME = "UpdateDataOnBackgroundWork"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val refreshDataResults = listOf(
            updateAnalyticsDashboardRangeSelections(),
            updateOrderListBySelectedStore(true)
        )
        return when {
            accountRepository.isUserLoggedIn().not() -> Result.success()
            refreshDataResults.all { it.isSuccess } -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.BACKGROUND_DATA_SYNCED,
                    mapOf(AnalyticsTracker.KEY_TIME_TAKEN to (System.currentTimeMillis() - startTime))
                )
                Result.success()
            }

            storeConnectionErrorMonitor.isDetectedForSelectedSite() -> {
                // The store can't be reached due to a server-side signature problem the app can't fix.
                // Stop the silent retry loop (it's re-enqueued the next time the app goes to background).
                WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
                Result.failure()
            }

            else -> {
                val errorDescription = refreshDataResults.filter { it.isFailure }.joinToString(" , ") {
                    it.exceptionOrNull()?.message
                        ?: "${UpdateDataOnBackgroundWorker::class.java.name} Unknown error"
                }

                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.BACKGROUND_DATA_SYNC_ERROR,
                    errorContext = this.javaClass.simpleName,
                    errorType = "BACKGROUND_DATA_SYNCED_ERROR",
                    errorDescription = errorDescription
                )
                Result.retry()
            }
        }
    }
}
