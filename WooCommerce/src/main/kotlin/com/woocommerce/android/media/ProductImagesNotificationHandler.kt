package com.woocommerce.android.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaUploadErrorListFragmentArgs
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailFragmentArgs
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.SystemServiceFactory
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
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val PRODUCT_UPDATE_NOTIFICATION_ID = 2
        private const val UPLOAD_FAILURE_NOTIFICATION_ID = 3

        private fun calculateProductUpdateNotificationId(productId: Long) =
            "$productId$PRODUCT_UPDATE_NOTIFICATION_ID".hashCode()

        private fun calculateUploadFailureNotificationId(productId: Long) =
            "$productId$UPLOAD_FAILURE_NOTIFICATION_ID".hashCode()
    }

    private val notificationManager =
        SystemServiceFactory.get(context, Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationBuilder: NotificationCompat.Builder

    init {
        createChannel()
        notificationBuilder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            color = ContextCompat.getColor(context, R.color.woo_gray_40)
            setSmallIcon(android.R.drawable.stat_sys_upload)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setProgress(0, 0, true)
            setGroup(FOREGROUND_NOTIFICATION_ID.toString())
        }
    }

    fun attachToService(service: ProductImagesService) {
        val notification = notificationBuilder.build()
        service.startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun attachToService(service: ProductImagesJobService, params: JobParameters) {
        val notification = notificationBuilder.build()
        service.setNotification(
            params,
            FOREGROUND_NOTIFICATION_ID,
            notification,
            JobService.JOB_END_NOTIFICATION_POLICY_REMOVE
        )
    }

    fun update(currentUpload: Int, totalUploads: Int) {
        val title = if (totalUploads == 1) {
            context.getString(R.string.product_images_uploading_single_notif_message)
        } else {
            context.getString(R.string.product_images_uploading_multi_notif_message, currentUpload, totalUploads)
        }

        notificationBuilder.apply {
            setContentTitle(title)
            setProgress(100, 0, false)
        }
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun setProgress(progress: Float) {
        notificationBuilder.apply {
            setProgress(100, (progress * 100).toInt(), false)
        }
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun showUpdatingProductNotification(product: Product?) {
        val title = context.getString(R.string.product_update_notification, product?.name.orEmpty())
            .replace("  ", " ")

        notificationBuilder.setContentTitle(title)
        notificationBuilder.setProgress(0, 0, true)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun postUpdateSuccessNotification(productId: Long, product: Product, imagesCount: Int) {
        val notificationBuilder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            color = ContextCompat.getColor(context, R.color.woo_gray_40)
            setSmallIcon(R.drawable.ic_done_secondary)
            setContentTitle(context.getString(R.string.product_update_success_notification_title))
            setContentText(
                context.getString(
                    R.string.product_update_success_notification_content,
                    imagesCount,
                    product.name
                )
            )
            setContentIntent(getProductDetailsIntent(productId))
            setAutoCancel(true)
            setGroup(PRODUCT_UPDATE_NOTIFICATION_ID.toString())
        }
        notificationManager.notify(calculateProductUpdateNotificationId(productId), notificationBuilder.build())
    }

    fun postUpdateFailureNotification(productId: Long, product: Product?) {
        val title = context.getString(R.string.product_update_failure_notification, product?.name.orEmpty())
            .replace("  ", " ")
        val notificationBuilder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            color = ContextCompat.getColor(context, R.color.woo_gray_40)
            setSmallIcon(android.R.drawable.stat_notify_error)
            setContentTitle(title)
            setContentIntent(getProductDetailsIntent(productId))
            setAutoCancel(true)
            setGroup(PRODUCT_UPDATE_NOTIFICATION_ID.toString())
        }
        notificationManager.notify(calculateProductUpdateNotificationId(productId), notificationBuilder.build())
    }

    fun postUploadFailureNotification(product: Product?, errors: List<ProductImageUploadData>) {
        val productId = product?.remoteId ?: errors.first().remoteProductId

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph_main)
            .setDestination(R.id.mediaUploadErrorsFragment)
            .setArguments(
                MediaUploadErrorListFragmentArgs(
                    remoteId = productId,
                    errorList = errors.toTypedArray()
                ).toBundle()
            )
            .createPendingIntent()

        val message = StringUtils.getQuantityString(
            context = context,
            quantity = errors.size,
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
            setContentTitle(product?.name)
            setContentText(message)
            setContentIntent(pendingIntent)
            setGroup(UPLOAD_FAILURE_NOTIFICATION_ID.toString())
        }
        notificationManager.notify(calculateUploadFailureNotificationId(productId), notificationBuilder.build())
    }

    fun removeUploadFailureNotification(productId: Long) {
        notificationManager.cancel(productId.toInt() + UPLOAD_FAILURE_NOTIFICATION_ID)
    }

    private fun getProductDetailsIntent(productId: Long) =
        NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph_main)
            .setDestination(R.id.productDetailFragment)
            .setArguments(
                ProductDetailFragmentArgs(
                    mode = ProductDetailFragment.Mode.ShowProduct(productId)
                ).toBundle()
            )
            .createPendingIntent()

    /**
     * Ensures the notification channel for image uploads is created - only required for Android O+
     */
    private fun createChannel() {
        // first check if the channel already exists
        notificationManager.getNotificationChannel(CHANNEL_ID)?.let {
            return
        }

        val channelName = context.getString(R.string.product_images_upload_channel_title)
        val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    fun removeForegroundNotification() {
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
    }
}
