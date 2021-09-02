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
        notifHandler.attachToService(this)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
