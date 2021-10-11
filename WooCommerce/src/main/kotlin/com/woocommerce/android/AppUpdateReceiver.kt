package com.woocommerce.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

/**
 * This is needed temporary to force re-registering the token with backend, to update the `selected_blog_id`
 */
class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WooLog.d(T.NOTIFS, "Received action ACTION_MY_PACKAGE_REPLACED, register notifications token")
            FCMRegistrationIntentService.enqueueWork(context)
        }
    }
}
