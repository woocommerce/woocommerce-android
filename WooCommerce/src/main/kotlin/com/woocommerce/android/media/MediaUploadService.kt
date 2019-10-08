package com.woocommerce.android.media

import android.content.Intent
import androidx.core.app.JobIntentService
import com.woocommerce.android.util.WooLog
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.ImageUtils
import javax.inject.Inject

/**
 * service which uploads photos to the WP media library
 */
class MediaUploadService : JobIntentService() {
    companion object {
        const val KEY_PRODUCT_ID = "key_product_id"
        const val KEY_LOCAL_MEDIA_FILENAME = "key_filename"
    }

    @Inject lateinit var mDispatcher: Dispatcher
    @Inject lateinit var mSiteStore: SiteStore

    // TODO: move to prefs?
    private val stripImageLocation = true
    private val optimizeImages = true
    private val maxImageSize = 2000
    private val imageQuality = 85

    override fun onCreate() {
        AndroidInjection.inject(this)
        mDispatcher.register(this)
        super.onCreate()
        WooLog.i(WooLog.T.MEDIA, "media upload service > created")
    }

    override fun onDestroy() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > destroyed")
        mDispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        val productId = intent.getLongExtra(KEY_PRODUCT_ID, 0L)
        val filename = intent.getStringExtra(KEY_LOCAL_MEDIA_FILENAME)

        if (optimizeImages) {
            val optimizedFilename = ImageUtils.optimizeImage(this, filename, maxImageSize, imageQuality)
        }
    }

    override fun onStopCurrentWork(): Boolean {
        // this Service was failing silently if it couldn't get to update its data, so
        // that hints us that we shouldn't really care about rescheduling this job
        // in the case something failed.
        return false
    }

    private fun dispatchUploadAction(media: MediaModel) {
        val site = mSiteStore.getSiteByLocalId(media.localSiteId)

        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
        val payload = UploadMediaPayload(site, media, stripImageLocation)
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.media == null) {
            WooLog.w(WooLog.T.MEDIA, "MediaUploadService > Received media event for null media, ignoring")
            return
        }

        if (event.isError) {
            // TODO
        } else {
            // TODO
        }
    }
}
