package com.woocommerce.android.push

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService

class FCMRegistrationIntentService : JobIntentService() {
    companion object {
        private const val JOB_FCM_REGISTRATION_SERVICE_ID = 1000

        fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, FCMRegistrationIntentService::class.java,
                    JOB_FCM_REGISTRATION_SERVICE_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        // TODO Register device and token with WordPress.com
    }

    override fun onStopCurrentWork(): Boolean {
        // Ensure that the job is rescheduled if stopped
        return true
    }
}
