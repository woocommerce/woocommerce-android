package com.woocommerce.android.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import org.wordpress.android.util.SystemServiceFactory
import java.util.Random

/**
 * Shows the standard uploading arrow animated notification icon to signify that images are being uploaded
 */
class ProductImagesNotificationHandler(
    val service: ProductImagesService,
    val remoteProductId: Long
) {
    companion object {
        private const val CHANNEL_ID = "image_upload_channel"
    }

    private val context: Context = service.baseContext.applicationContext
    private val notificationId: Int
    private val notificationManager: NotificationManager
    private val notificationBuilder: NotificationCompat.Builder

    init {
        notificationManager = SystemServiceFactory.get(
                context,
                Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        createChannel()

        notificationBuilder = NotificationCompat.Builder(
                context,
                CHANNEL_ID
        ).also {
            it.color = ContextCompat.getColor(context, R.color.woo_gray_40)
            it.setSmallIcon(android.R.drawable.stat_sys_upload)
            it.setOnlyAlertOnce(true)
            it.setOngoing(true)
            it.setProgress(0, 0, true)
        }

        val notification = notificationBuilder.build()
        notificationId = (Random()).nextInt()
        service.startForeground(notificationId, notification)
        notificationManager.notify(notificationId, notification)
    }

    fun setProgress(progress: Int) {
        notificationBuilder.setProgress(100, progress, false)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun update(currentUpload: Int, totalUploads: Int) {
        val title = if (totalUploads == 1) {
            context.getString(R.string.product_images_uploading_single_notif_message)
        } else {
            context.getString(R.string.product_images_uploading_multi_notif_message, currentUpload, totalUploads)
        }

        notificationBuilder.setContentTitle(title)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Removes the notification, called after all images have been uploaded
     */
    fun remove() {
        notificationManager.cancel(notificationId)
    }

    /**
     * Ensures the notification channel for image uploads is created - only required for Android O+
     */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // first check if the channel already exists
            notificationManager.getNotificationChannel(CHANNEL_ID)?.let {
                return
            }

            val channelName = context.getString(R.string.product_images_upload_channel_title)
            val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
