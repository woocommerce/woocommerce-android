package com.woocommerce.android.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OrderNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateOrderAndOrderList: UpdateOrderAndOrderList,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val SITE_ID = "site_id"
        const val ORDER_ID = "order_id"
        fun schedule(context: Context, siteId: Long, remoteOrderId: Long) {
            val dataBuilder = Data.Builder()
            dataBuilder.putLong(SITE_ID, siteId)
            dataBuilder.putLong(ORDER_ID, remoteOrderId)
            val data = dataBuilder.build()

            val workRequest = OneTimeWorkRequest.Builder(OrderNotificationWorker::class.java)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        val siteId = inputData.getLong(SITE_ID, -1L)
        val orderId = inputData.getLong(ORDER_ID, -1L)

        if (siteId == -1L || orderId == -1L) return Result.success()

        return updateOrderAndOrderList(siteId, orderId).fold(
            onSuccess = {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.PUSH_NOTIFICATION_ORDER_BACKGROUND_SYNCED,
                    mapOf(AnalyticsTracker.KEY_TIME_TAKEN to (System.currentTimeMillis() - startTime))
                )
                Result.success()
            },
            onFailure = { exception ->
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.PUSH_NOTIFICATION_ORDER_BACKGROUND_SYNC_ERROR,
                    errorContext = this.javaClass.simpleName,
                    errorType = "ORDER_NOTIFICATION_SYNCED_ERROR",
                    errorDescription = exception.message
                )
                Result.failure()
            }
        )
    }
}
