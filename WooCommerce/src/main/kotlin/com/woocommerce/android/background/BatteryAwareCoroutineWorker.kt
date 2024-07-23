package com.woocommerce.android.background

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.ui.main.MainActivity

abstract class BatteryAwareCoroutineWorker(
    protected val appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    abstract val notificationId: Int

    abstract val titleResId: Int

    abstract val messageResId: Int

    abstract suspend fun executeWork(): Result

    override suspend fun doWork(): Result {
        if (isPowerSaveModeEnabled().not()) {
            val notification = createForegroundNotification(
                title = applicationContext.getString(titleResId),
                message = applicationContext.getString(messageResId)
            )
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            val foregroundInfo = ForegroundInfo(
                notificationId,
                notification,
                foregroundServiceType
            )
            setForeground(foregroundInfo)
        }
        return executeWork()
    }

    private fun isPowerSaveModeEnabled(): Boolean {
        return (appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
    }

    private fun createForegroundNotification(title: String, message: String): Notification {
        val id = applicationContext.getString(R.string.notification_channel_background_works_id)
        val notificationIntent = Intent(appContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(appContext, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(appContext, id)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_woo_bubble) // Replace with your icon resource
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set priority for foreground notification
            .build()
    }
}
