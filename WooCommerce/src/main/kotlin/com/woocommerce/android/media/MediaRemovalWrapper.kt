package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.woocommerce.android.JobServiceIds
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for the media removal service
 */
@Singleton
class MediaRemovalWrapper
@Inject constructor(private val context: Context) {
    fun removeProductMedia(remoteProductId: Long, remoteMediaId: Long) {
        val intent = Intent(context, MediaRemovalService::class.java).also {
            it.putExtra(MediaRemovalService.KEY_REMOTE_PRODUCT_ID, remoteProductId)
            it.putExtra(MediaRemovalService.KEY_REMOTE_MEDIA_ID, remoteMediaId)
        }
        JobIntentService.enqueueWork(
                context,
                MediaUploadService::class.java,
                JobServiceIds.JOB_REMOVE_PRODUCT_MEDIA_SERVICE_ID,
                intent
        )
    }
}
