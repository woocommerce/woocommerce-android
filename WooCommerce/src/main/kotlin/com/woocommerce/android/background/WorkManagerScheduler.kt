package com.woocommerce.android.background

import android.content.Context
import javax.inject.Inject

class WorkManagerScheduler @Inject constructor(
    private val context: Context
) {
    fun scheduleOrderUpdate(siteId: Long, remoteOrderId: Long) {
        OrderNotificationWorker.schedule(context, siteId, remoteOrderId)
    }
}
