package com.woocommerce.android.push

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

class NotificationsProcessingService : Service() {
    companion object {
        const val ARG_ACTION_TYPE = "action_type"
        const val ARG_ACTION_NOTIFICATION_DISMISS = "action_dismiss"
        const val ARG_PUSH_ID = "notificationId"

        fun getPendingIntentForNotificationDismiss(context: Context, pushId: Int): PendingIntent {
            val intent = Intent(context, NotificationsProcessingService::class.java)
            intent.putExtra(ARG_ACTION_TYPE, ARG_ACTION_NOTIFICATION_DISMISS)
            intent.putExtra(ARG_PUSH_ID, pushId)
            intent.addCategory(ARG_ACTION_NOTIFICATION_DISMISS)
            return PendingIntent.getService(context, pushId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }

    private lateinit var actionProcessor: ActionProcessor

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        WooLog.i(T.NOTIFS, "NotificationsProcessingService > created")
    }

    override fun onDestroy() {
        WooLog.i(T.NOTIFS, "NotificationsProcessingService > destroyed")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Offload to a separate thread.
        actionProcessor = ActionProcessor(this, intent, startId)
        Thread(Runnable { actionProcessor.process() }).start()

        return Service.START_NOT_STICKY
    }

    private inner class ActionProcessor internal constructor(
        private val context: Context,
        private val intent: Intent?,
        private val taskId: Int
    ) {
        fun process() {
            intent?.getStringExtra(ARG_ACTION_TYPE)?.let { actionType ->
                // Check notification dismissed pending intent
                if (actionType == ARG_ACTION_NOTIFICATION_DISMISS) {
                    val notificationId = intent.getIntExtra(ARG_PUSH_ID, 0)
                    if (notificationId == NotificationHandler.GROUP_NOTIFICATION_ID) {
                        NotificationHandler.clearNotifications()
                    } else {
                        NotificationHandler.removeNotification(notificationId)
                        // Dismiss the grouped notification if a user dismisses all notifications from a wear device
                        if (!NotificationHandler.hasNotifications()) {
                            val notificationManager = NotificationManagerCompat.from(context)
                            notificationManager.cancel(NotificationHandler.GROUP_NOTIFICATION_ID)
                        }
                    }
                }
            } ?: stopSelf(taskId)
        }
    }
}
