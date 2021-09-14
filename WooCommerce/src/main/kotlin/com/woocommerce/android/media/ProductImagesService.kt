package com.woocommerce.android.media

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Service which uploads device images to the WP media library to be later assigned to a product
 */
@AndroidEntryPoint
class ProductImagesService : Service() {
    @Inject lateinit var notifHandler: ProductImagesNotificationHandler

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // If the system restarts the service, we won't be able to restore the state of uploads,
            // so we'll just stop it
            stopSelf()
        } else {
            notifHandler.attachToService(this)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // After testing, sometimes the notification gets stuck after stopping the service if it wasn't
        // removed explicitly
        notifHandler.removeForegroundNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
