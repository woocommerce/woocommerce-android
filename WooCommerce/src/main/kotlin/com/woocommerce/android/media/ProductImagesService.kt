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
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

/**
 * Service which uploads device images to the WP media library to be later assigned to a product
 */
class ProductImagesService : JobIntentService() {
    companion object {
        const val KEY_REMOTE_PRODUCT_ID = "key_remote_product_id"
        const val KEY_LOCAL_URI_LIST = "key_local_uri_list"

        private const val STRIP_LOCATION = true
        private const val TIMEOUT_PER_UPLOAD = 120L

        // array of remoteProductId / uploading image uris for that product
        private val currentUploads = LongSparseArray<ArrayList<Uri>>()

        // posted when the list of images starts uploading
        class OnProductImagesUpdateStartedEvent(
            val remoteProductId: Long
        )

        // posted when the list of images finishes uploading
        class OnProductImagesUpdateCompletedEvent(
            val remoteProductId: Long
        )

        // posted when a single image has been uploaded
        class OnProductImageUploaded(
            val media: MediaModel? = null,
            val isError: Boolean = false
        )

        fun isUploadingForProduct(remoteProductId: Long) = currentUploads.containsKey(remoteProductId)

        fun isBusy() = !currentUploads.isEmpty

        fun getUploadingImageUrisForProduct(remoteProductId: Long): List<Uri> {
            return currentUploads.get(remoteProductId) ?: emptyList()
        }
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var networkStatus: NetworkStatus

    private var doneSignal: CountDownLatch? = null

    private lateinit var notifHandler: ProductImagesNotificationHandler

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
        if (!networkStatus.isConnected()) {
            return
        }

        val localUriList = intent.getParcelableArrayListExtra<Uri>(KEY_LOCAL_URI_LIST)
        if (localUriList.isNullOrEmpty()) {
            WooLog.w(T.MEDIA, "productImagesService > null media list")
            return
        }

        // set the uploads for this product
        currentUploads.put(remoteProductId, localUriList)

        // post an event that the upload is starting
        val event = OnProductImagesUpdateStartedEvent(remoteProductId)
        EventBus.getDefault().post(event)

        val totalUploads = localUriList.size
        notifHandler = ProductImagesNotificationHandler(this, remoteProductId)

        for (index in 0 until totalUploads) {
            notifHandler.update(index + 1, totalUploads)
            val localUri = localUriList[index]

            // create a media model from this local image uri
            val media = ProductImagesUtils.mediaModelFromLocalUri(
                    this,
                    selectedSite.get().id,
                    localUri,
                    mediaStore
            )
            if (media == null) {
                WooLog.w(T.MEDIA, "productImagesService > null media")
                handleFailure()
            } else {
                media.postId = remoteProductId
                media.setUploadState(MediaModel.MediaUploadState.UPLOADING)

                // dispatch the upload request
                WooLog.d(T.MEDIA, "productImagesService > Dispatching request to upload $localUri")
                val payload = UploadMediaPayload(selectedSite.get(), media, STRIP_LOCATION)
                dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))

                // wait for the upload to complete
                try {
                    doneSignal = CountDownLatch(1)
                    doneSignal!!.await(TIMEOUT_PER_UPLOAD, SECONDS)
                } catch (e: InterruptedException) {
                    WooLog.e(T.MEDIA, "productImagesService > interrupted", e)
                }
            }

            // remove this uri from the list of uploads for this product
            currentUploads.get(remoteProductId)?.let { oldList ->
                val newList = ArrayList<Uri>().also {
                    it.addAll(oldList)
                    it.remove(localUri)
                }
                currentUploads.put(remoteProductId, newList)
            }
        }

        currentUploads.remove(remoteProductId)
        productImageMap.remove(remoteProductId)

        // remove the notification and alert that all uploads have completed
        notifHandler.remove()
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(remoteProductId))
    }

    override fun onStopCurrentWork(): Boolean {
        super.onStopCurrentWork()
        WooLog.i(T.MEDIA, "productImagesService > onStopCurrentWork")
        return true
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        when {
            event.isError -> {
                WooLog.w(
                        T.MEDIA,
                        "productImagesService > error uploading media: ${event.error.type}, ${event.error.message}"
                )
                handleFailure()
            }
            event.canceled -> {
                WooLog.w(T.MEDIA, "productImagesService > upload media cancelled")
                handleFailure()
            }
            event.completed -> {
                WooLog.i(T.MEDIA, "productImagesService > uploaded media ${event.media?.id}")
                handleSuccess(event.media)
            } else -> {
                // otherwise this is an upload progress event, so update the notification progress
                val progress = (event.progress * 100).toInt()
                notifHandler.setProgress(progress)
            }
        }
    }

    private fun handleSuccess(uploadedMedia: MediaModel) {
        countDown()
        EventBus.getDefault().post(OnProductImageUploaded(uploadedMedia))
    }

    private fun handleFailure() {
        countDown()
        EventBus.getDefault().post(OnProductImageUploaded(isError = true))
    }

    private fun countDown() {
        doneSignal?.let {
            if (it.count > 0) {
                it.countDown()
            }
        }
    }

    // TODO: this should be removed before submitting this PR
    /**
     * Called after device media has been uploaded to dispatch a request to assign the uploaded media
     * to the product
     */
    /*private fun dispatchAddMediaAction(media: MediaModel) {
        val product = productStore.getProductByRemoteId(selectedSite.get(), media.postId)
        if (product == null) {
            WooLog.w(T.MEDIA, "productImagesService > product is null")
            handleFailure()
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
            handleFailure()
        } else {
            WooLog.i(T.MEDIA, "productImagesService > product images changed")
            handleSuccess()
        }
    }*/
}
