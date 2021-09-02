package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import com.woocommerce.android.JobServiceIds
import com.woocommerce.android.model.Product
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the product images service - it's sole purpose is to have an injected context so viewModels
 * can upload media to the service without requiring a context
 */
@Singleton
class ProductImagesServiceWrapper
@Inject constructor(
    private val context: Context,
    private val productImagesNotificationHandler: ProductImagesNotificationHandler
) {
    fun startService() {
        ContextCompat.startForegroundService(context, Intent(context, ProductImagesService::class.java))
    }

    fun stopService() {
        context.stopService(Intent(context, ProductImagesService::class.java))
    }

    fun showUploadFailureNotification(productId: Long, failuresCount: Int) {
        productImagesNotificationHandler.postUploadFailureNotification(productId, failuresCount)
    }

    fun removeUploadFailureNotification(productId: Long) {
        productImagesNotificationHandler.removeUploadFailureNotification(productId)
    }
}
