package com.woocommerce.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.woocommerce.android.JobServiceIds.JOB_UPLOAD_PRODUCT_MEDIA_SERVICE_ID
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

/**
 * service which uploads photos to the WP media library
 */
class MediaUploadService : JobIntentService() {
    companion object {
        private const val KEY_PRODUCT_ID = "key_product_id"
        private const val KEY_LOCAL_MEDIA_URI = "key_media_uri"

        fun uploadProductMedia(context: Context, productId: Long, localMediaUri: Uri) {
            val intent = Intent(context, MediaUploadService::class.java)
            intent.putExtra(KEY_PRODUCT_ID, productId)
            intent.putExtra(KEY_LOCAL_MEDIA_URI, localMediaUri)
            enqueueWork(
                    context,
                    MediaUploadService::class.java,
                    JOB_UPLOAD_PRODUCT_MEDIA_SERVICE_ID,
                    intent
            )
        }
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var selectedSite: SelectedSite

    private val doneSignal = CountDownLatch(1)

    // TODO: move to prefs?
    private val stripImageLocation = true
    private val optimizeImages = true
    private val maxImageSize = 2000
    private val imageQuality = 85

    override fun onCreate() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > created")
        AndroidInjection.inject(this)
        dispatcher.register(this)
        super.onCreate()
     }

    override fun onDestroy() {
        WooLog.i(WooLog.T.MEDIA, "media upload service > destroyed")
        dispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        WooLog.i(WooLog.T.MEDIA, "media upload service > onHandleWork")

        val productId = intent.getLongExtra(KEY_PRODUCT_ID, 0L)
        var localMediaUri = intent.getParcelableExtra<Uri>(KEY_LOCAL_MEDIA_URI)

        /*if (optimizeImages) {
            filename = ImageUtils.optimizeImage(this, filename, maxImageSize, imageQuality)
        }*/

        val media = MediaUploadUtils.mediaModelFromLocalUri(
                this,
                selectedSite.get().id,
                localMediaUri,
                mediaStore
        )

        // TODO: handle null media
        media?.let {
            it.postId = productId
            it.setUploadState(MediaModel.MediaUploadState.UPLOADING)
            dispatchUploadAction(it)
        }
    }

    override fun onStopCurrentWork(): Boolean {
        super.onStopCurrentWork()
        WooLog.i(WooLog.T.MEDIA, "media upload service > onStopCurrentWork")
        return true
    }

    private fun dispatchUploadAction(media: MediaModel) {
        val site = siteStore.getSiteByLocalId(media.localSiteId)
        val payload = UploadMediaPayload(site, media, stripImageLocation)
        dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
        doneSignal.await()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError) {
            WooLog.w(WooLog.T.MEDIA, "MediaUploadService > error uploading media: ${event.error.type}, ${event.error.message}")
            // TODO
        } else {
            WooLog.w(WooLog.T.MEDIA, "MediaUploadService > uploaded media ${event.media?.id}")
            // TODO
        }

        doneSignal.countDown()
    }
}
