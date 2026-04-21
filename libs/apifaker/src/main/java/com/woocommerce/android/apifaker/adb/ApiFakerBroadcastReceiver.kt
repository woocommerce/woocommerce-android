package com.woocommerce.android.apifaker.adb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.woocommerce.android.apifaker.LOG_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ApiFakerBroadcastReceiver : BroadcastReceiver() {
    @Inject
    internal lateinit var actionHandler: BroadcastActionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                actionHandler.handle(intent)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(LOG_TAG, "ADB: Command failed: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
