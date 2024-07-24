package com.woocommerce.android.background

import android.content.Context
import javax.inject.Inject

class WorkManagerScheduler @Inject constructor(
    private val context: Context
) {
    fun scheduleOrderUpdate(remoteOrderId: Long) {
        OrderNotificationWorker.schedule(context, remoteOrderId)
    }
}
