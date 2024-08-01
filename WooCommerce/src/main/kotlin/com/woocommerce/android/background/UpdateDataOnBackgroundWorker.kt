package com.woocommerce.android.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UpdateDataOnBackgroundWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val updateAnalyticsDashboardRangeSelections: UpdateAnalyticsDashboardRangeSelections,
    private val updateOrderListBySelectedStore: UpdateOrderListBySelectedStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val REFRESH_TIME = 4L
        const val WORK_NAME = "UpdateDataOnBackgroundWork"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val updateAnalyticsDashboardRangeSelectionsResult = updateAnalyticsDashboardRangeSelections()
        val updateOrderListBySelectedStoreResult = updateOrderListBySelectedStore(true)
        return when {
            accountRepository.isUserLoggedIn().not() -> Result.success()
            updateAnalyticsDashboardRangeSelectionsResult && updateOrderListBySelectedStoreResult -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.BACKGROUND_DATA_SYNCED,
                    mapOf(AnalyticsTracker.KEY_TIME_TAKEN to (System.currentTimeMillis() - startTime))
                )
                Result.success()
            }

            else -> {
                val errorDescription = when {
                    updateAnalyticsDashboardRangeSelectionsResult.not() &&
                        updateOrderListBySelectedStoreResult.not() -> {
                        "Orders & Dashboard stats refresh failed."
                    }

                    updateAnalyticsDashboardRangeSelectionsResult.not() -> {
                        "Dashboard stats refresh failed."
                    }

                    else -> "Orders refresh failed."
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
