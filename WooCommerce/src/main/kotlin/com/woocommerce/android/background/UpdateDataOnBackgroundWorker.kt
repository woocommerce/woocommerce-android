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
        return when {
            accountRepository.isUserLoggedIn().not() -> Result.success()
            updateAnalyticsDashboardRangeSelections() && updateOrderListBySelectedStore(true) -> {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.BACKGROUND_DATA_SYNCED,
                    mapOf(AnalyticsTracker.KEY_TIME_TAKEN to (System.currentTimeMillis() - startTime))
                )
                Result.success()
            }

            else -> {
                analyticsTrackerWrapper.track(AnalyticsEvent.BACKGROUND_DATA_SYNC_ERROR)
                Result.retry()
            }
        }
    }
}
