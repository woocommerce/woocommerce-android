package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.woocommerce.android.JobServiceIds
import com.woocommerce.android.media.MediaUploadService.Companion.MediaAction.ACTION_REMOVE
import com.woocommerce.android.media.MediaUploadService.Companion.MediaAction.ACTION_ADD
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the media upload service - it's sole purpose is to have an injected context so viewModels
 * can upload media to the service without requiring a context
 */
@Singleton
class MediaUploadWrapper
@Inject constructor(private val context: Context) {
    fun uploadProductMedia(remoteProductId: Long, localMediaUri: Uri) {
        val intent = Intent(context, MediaUploadService::class.java).also {
            it.putExtra(MediaUploadService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(MediaUploadService.KEY_LOCAL_MEDIA_URI, localMediaUri)
            it.putExtra(MediaUploadService.KEY_ACTION, ACTION_ADD)
        }
        JobIntentService.enqueueWork(
                context,
                MediaUploadService::class.java,
                JobServiceIds.JOB_UPLOAD_PRODUCT_MEDIA_SERVICE_ID,
                intent
        )
    }

    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        val intent = Intent(context, MediaUploadService::class.java).also {
            it.putExtra(MediaUploadService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(MediaUploadService.KEY_REMOTE_MEDIA_ID, remoteMediaId)
            it.putExtra(MediaUploadService.KEY_ACTION, ACTION_REMOVE)
        }
        JobIntentService.enqueueWork(
                context,
                MediaUploadService::class.java,
                JobServiceIds.JOB_UPLOAD_PRODUCT_MEDIA_SERVICE_ID,
                intent
        )
    }
}
