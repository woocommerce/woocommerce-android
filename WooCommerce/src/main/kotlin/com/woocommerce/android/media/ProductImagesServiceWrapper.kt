package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
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
@Inject constructor(private val context: Context) {
    fun uploadProductMedia(remoteProductId: Long, localMediaUriList: ArrayList<Uri>) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.action = ProductImagesService.ACTION_UPLOAD_IMAGES
            it.putExtra(ProductImagesService.KEY_ID, remoteProductId)
            it.putParcelableArrayListExtra(ProductImagesService.KEY_LOCAL_URI_LIST, localMediaUriList)
        }
        JobIntentService.enqueueWork(
            context,
            ProductImagesService::class.java,
            JobServiceIds.JOB_PRODUCT_IMAGES_SERVICE_ID,
            intent
        )
    }

    fun addImagesToProduct(remoteProductId: Long, images: List<Product.Image>) {
        val intent = Intent(context, ProductImagesService::class.java).also {
            it.action = ProductImagesService.ACTION_UPDATE_PRODUCT
            it.putExtra(ProductImagesService.KEY_ID, remoteProductId)
            it.putParcelableArrayListExtra(ProductImagesService.KEY_UPLOADED_IMAGES, ArrayList(images))
        }
        JobIntentService.enqueueWork(
            context,
            ProductImagesService::class.java,
            JobServiceIds.JOB_PRODUCT_IMAGES_SERVICE_ID,
            intent
        )
    }
}
