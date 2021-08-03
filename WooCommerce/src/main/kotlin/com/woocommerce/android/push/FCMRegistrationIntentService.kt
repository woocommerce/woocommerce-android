package com.woocommerce.android.push

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.google.firebase.messaging.FirebaseMessaging
import com.woocommerce.android.JobServiceIds.JOB_FCM_REGISTRATION_SERVICE_ID
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCMRegistrationIntentService : JobIntentService() {
    @Inject internal lateinit var notificationRegistrationHandler: NotificationRegistrationHandler

    companion object {
        fun enqueueWork(context: Context) {
            val work = Intent(context, FCMRegistrationIntentService::class.java)
            enqueueWork(
                context, FCMRegistrationIntentService::class.java,
                JOB_FCM_REGISTRATION_SERVICE_ID, work
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = task.result

            token?.takeIf { it.isNotEmpty() }?.let {
                WooLog.d(WooLog.T.NOTIFS, "Sending FCM token to our remote services: $it")
                notificationRegistrationHandler.onNewFCMTokenReceived(it)
            } ?: run {
                WooLog.w(WooLog.T.NOTIFS, "Empty FCM token, can't register the id on remote services")
                notificationRegistrationHandler.onEmptyFCMTokenReceived()
            }
        }
    }

    override fun onStopCurrentWork(): Boolean {
        // Ensure that the job is rescheduled if stopped
        return true
    }
}
