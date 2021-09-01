package com.woocommerce.android.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.media.MediaUploadErrorListFragmentArgs
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.SystemServiceFactory
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows the standard uploading arrow animated notification icon to signify that images are being uploaded
 */
@Singleton
class ProductImagesNotificationHandler @Inject constructor(
    val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "image_upload_channel"
    }

    private val notificationManager =
        SystemServiceFactory.get(context, Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var notificationId: Int = 0

    init {
        createChannel()
    }

    fun attachToService(service: ProductImagesService) {
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

    fun postProductUpdateNotification(product: Product?) {
        val title = context.getString(R.string.product_update_notification, product?.name.orEmpty())
        notificationBuilder.setContentTitle(title)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun postUpdateFailureNotification(productId: Long, product: Product?) {
        val notificationBuilder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            color = ContextCompat.getColor(context, R.color.woo_gray_40)
            setSmallIcon(android.R.drawable.stat_notify_error)
            setContentTitle(context.getString(R.string.product_update_failure_notification, product?.name.orEmpty()))
        }
        notificationManager.notify(productId.toInt(), notificationBuilder.build())
    }

    fun postUploadFailureNotification(productId: Long, failuresCount: Int) {
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph_main)
            .setDestination(R.id.mediaUploadErrorsFragment)
            .setArguments(MediaUploadErrorListFragmentArgs(remoteId = productId).toBundle())
            .createPendingIntent()

        val message = StringUtils.getQuantityString(
            context = context,
            quantity = failuresCount,
            default = R.string.product_image_service_error_uploading_multiple,
            one = R.string.product_image_service_error_uploading_single,
            zero = R.string.product_image_service_error_uploading
        )

        val notificationBuilder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            color = ContextCompat.getColor(context, R.color.woo_gray_40)
            setSmallIcon(android.R.drawable.stat_notify_error)
            setContentText(message)
            setContentIntent(pendingIntent)
        }
        notificationManager.notify(productId.toInt(), notificationBuilder.build())
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
