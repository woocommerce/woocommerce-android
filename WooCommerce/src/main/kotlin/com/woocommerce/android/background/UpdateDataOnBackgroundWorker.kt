package com.woocommerce.android.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.ui.login.AccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UpdateDataOnBackgroundWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val updateAnalyticsDashboardRangeSelections: UpdateAnalyticsDashboardRangeSelections,
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val REFRESH_TIME = 30L
        const val WORK_NAME = "UpdateDataOnBackgroundWork"
    }

    override suspend fun doWork(): Result {
        return when {
            accountRepository.isUserLoggedIn().not() -> Result.success()
            updateAnalyticsDashboardRangeSelections() -> Result.success()
            else -> Result.retry()
        }
    }
}
