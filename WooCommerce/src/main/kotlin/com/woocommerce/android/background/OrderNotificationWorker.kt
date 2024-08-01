package com.woocommerce.android.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OrderNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateOrderAndOrderList: UpdateOrderAndOrderList
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
        val siteId = inputData.getLong(SITE_ID, -1L)
        val orderId = inputData.getLong(ORDER_ID, -1L)
        return when {
            orderId == -1L -> Result.failure()
            updateOrderAndOrderList(siteId, orderId) -> Result.success()
            else -> Result.failure()
        }
    }
}
