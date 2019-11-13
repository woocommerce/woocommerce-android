package com.woocommerce.android.media

import android.content.Intent
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

/**
 * service which adds or removes a product image
 */
class ProductImagesService : JobIntentService() {
    companion object {
        const val KEY_REMOTE_PRODUCT_ID = "key_remote_product_id"
        const val KEY_LOCAL_MEDIA_URI = "key_local_media_uri"

        private const val STRIP_LOCATION = true

        // array of remoteProductId / localImageUri
        private val currentUploads = LongSparseArray<Uri>()

        class OnProductImagesUpdateStartedEvent(
            var remoteProductId: Long
        )

        class OnProductImagesUpdateCompletedEvent(
            var remoteProductId: Long,
            val isError: Boolean
        )

        fun isUploadingForProduct(remoteProductId: Long) = currentUploads.containsKey(remoteProductId)

        fun isBusy() = !currentUploads.isEmpty
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var networkStatus: NetworkStatus

    private val doneSignal = CountDownLatch(1)

    override fun onCreate() {
        WooLog.i(T.MEDIA, "productImagesService > created")
        AndroidInjection.inject(this)
        dispatcher.register(this)
        super.onCreate()
    }

    override fun onDestroy() {
        WooLog.i(T.MEDIA, "productImagesService > destroyed")
        dispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        WooLog.i(T.MEDIA, "productImagesService > onHandleWork")

        val remoteProductId = intent.getLongExtra(KEY_REMOTE_PRODUCT_ID, 0L)
        val localMediaUri = intent.getParcelableExtra<Uri>(KEY_LOCAL_MEDIA_URI)
        if (localMediaUri == null) {
            WooLog.w(T.MEDIA, "productImagesService > null localMediaUri")
            handleFailure(remoteProductId)
            return
        }

        if (!networkStatus.isConnected()) {
            handleFailure(remoteProductId)
            return
        }

        ProductImagesUtils.mediaModelFromLocalUri(
                this,
                selectedSite.get().id,
                localMediaUri,
                mediaStore
        )?.let { media ->
            media.postId = remoteProductId
            media.setUploadState(MediaModel.MediaUploadState.UPLOADING)
            currentUploads.put(remoteProductId, localMediaUri)

            // first fire an event that the upload is starting
            EventBus.getDefault().post(OnProductImagesUpdateStartedEvent(remoteProductId))

            // then dispatch the upload request
            val site = siteStore.getSiteByLocalId(media.localSiteId)
            val payload = UploadMediaPayload(site, media, STRIP_LOCATION)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
            doneSignal.await()
            return
        }

        WooLog.w(T.MEDIA, "productImagesService > null media")
        handleFailure(remoteProductId)
    }

    override fun onStopCurrentWork(): Boolean {
        super.onStopCurrentWork()
        WooLog.i(T.MEDIA, "productImagesService > onStopCurrentWork")
        return true
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        val remoteProductId = event.media?.postId ?: 0L
        when {
            event.isError -> {
                WooLog.w(
                        T.MEDIA,
                        "productImagesService > error uploading media: ${event.error.type}, ${event.error.message}"
                )
                handleFailure(remoteProductId)
            }
            event.canceled -> {
                WooLog.w(T.MEDIA, "productImagesService > upload media cancelled")
                handleFailure(remoteProductId)
            }
            event.completed -> {
                dispatchAddMediaAction(event.media)
                WooLog.i(T.MEDIA, "productImagesService > uploaded media ${event.media?.id}")
            }
        }
    }

    /**
     * Called after device media has been uploaded to dispatch a request to assign the uploaded media
     * to the product
     */
    private fun dispatchAddMediaAction(media: MediaModel) {
        val product = productStore.getProductByRemoteId(selectedSite.get(), media.postId)
        if (product == null) {
            WooLog.w(T.MEDIA, "productImagesService > product is null")
            handleFailure(media.postId)
        } else {
            // add the new image as the first (primary) one
            val imageList = ArrayList<WCProductImageModel>().also {
                it.add(WCProductImageModel.fromMediaModel(media))
                it.addAll(product.getImages())
            }
            val site = siteStore.getSiteByLocalId(media.localSiteId)
            val payload = UpdateProductImagesPayload(site, media.postId, imageList)
            dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        if (event.isError) {
            WooLog.w(
                    T.MEDIA,
                    "productImagesService > error changing product images: ${event.error.type}, ${event.error.message}"
            )
            handleFailure(event.remoteProductId)
        } else {
            WooLog.i(T.MEDIA, "productImagesService > product images changed")
            handleSuccess(event.remoteProductId)
        }
    }

    private fun handleSuccess(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(remoteProductId, isError = false))
        doneSignal.countDown()
        productImageMap.remove(remoteProductId)
    }

    private fun handleFailure(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(remoteProductId, isError = true))
        currentUploads.remove(remoteProductId)
    }
}
