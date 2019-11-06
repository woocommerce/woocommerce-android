package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.woocommerce.android.JobServiceIds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the product images service - it's sole purpose is to have an injected context so viewModels
 * can upload media to the service without requiring a context
 */
@Singleton
class ProductImagesWrapper
@Inject constructor(private val context: Context) {
    fun uploadProductMedia(remoteProductId: Long, localMediaUri: Uri) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.putExtra(ProductImagesService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(ProductImagesService.KEY_LOCAL_MEDIA_URI, localMediaUri)
        }
        JobIntentService.enqueueWork(
                context,
                ProductImagesService::class.java,
                JobServiceIds.JOB_UPLOAD_PRODUCT_MEDIA_SERVICE_ID,
                intent
        )
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.putExtra(ProductImagesService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(ProductImagesService.KEY_REMOTE_MEDIA_ID, remoteMediaId)
        }
        JobIntentService.enqueueWork(
                context,
                ProductImagesService::class.java,
                JobServiceIds.JOB_REMOVE_PRODUCT_MEDIA_SERVICE_ID,
                intent
        )
    }
}
