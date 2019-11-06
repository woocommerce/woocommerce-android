package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.woocommerce.android.JobServiceIds
import com.woocommerce.android.media.ProductImagesService.Companion.Action
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the product images service - it's sole purpose is to have an injected context so viewModels
 * can upload media to the service without requiring a context
 */
@Singleton
class ProductImagesServiceWrapper
@Inject constructor(private val context: Context) {
    fun uploadProductMedia(remoteProductId: Long, localMediaUri: Uri) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.putExtra(ProductImagesService.KEY_ACTION, Action.UPLOAD_IMAGE)
            it.putExtra(ProductImagesService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(ProductImagesService.KEY_LOCAL_MEDIA_URI, localMediaUri)
        }
        JobIntentService.enqueueWork(
                context,
                ProductImagesService::class.java,
                JobServiceIds.JOB_PRODUCT_IMAGES_SERVICE_ID,
                intent
        )
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.putExtra(ProductImagesService.KEY_ACTION, Action.REMOVE_IMAGE)
            it.putExtra(ProductImagesService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(ProductImagesService.KEY_REMOTE_MEDIA_ID, remoteMediaId)
        }
        JobIntentService.enqueueWork(
                context,
                ProductImagesService::class.java,
                JobServiceIds.JOB_PRODUCT_IMAGES_SERVICE_ID,
                intent
        )
    }
}
