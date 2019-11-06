package com.woocommerce.android.media

import android.content.Intent
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
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
        const val KEY_ACTION = "action"
        const val KEY_REMOTE_PRODUCT_ID = "key_remote_product_id"
        const val KEY_REMOTE_MEDIA_ID = "key_remote_media_id"
        const val KEY_LOCAL_MEDIA_URI = "key_local_media_uri"

        enum class Action {
            NONE,
            UPLOAD_IMAGE,
            REMOVE_IMAGE
        }
        private var currentAction = Action.NONE

        private const val STRIP_LOCATION = true

        // array of remoteProductId / localImageUri
        private val currentUploads = LongSparseArray<Uri>()

        // array of remoteProductId / remoteMediaId
        private val currentRemovals = LongSparseArray<Long>()

        class OnProductImagesUpdateStartedEvent(
            var action: Action,
            var remoteProductId: Long
        )

        class OnProductImagesUpdateCompletedEvent(
            var action: Action,
            var remoteProductId: Long,
            val isError: Boolean
        )

        fun isUploadingForProduct(remoteProductId: Long) = currentUploads.containsKey(remoteProductId)

        fun isBusy() = currentAction != Action.NONE
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap

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

        currentAction = intent.getSerializableExtra(KEY_ACTION) as Action
        val remoteProductId = intent.getLongExtra(KEY_REMOTE_PRODUCT_ID, 0L)

        when (currentAction) {
            Action.UPLOAD_IMAGE -> handleUpload(intent, remoteProductId)
            Action.REMOVE_IMAGE -> handleRemoval(intent, remoteProductId)
            else -> {
                WooLog.w(T.MEDIA, "productImagesService > unsupported action")
                handleFailure(remoteProductId)
            }
        }
    }

    private fun handleUpload(intent: Intent, remoteProductId: Long) {
        val localMediaUri = intent.getParcelableExtra<Uri>(KEY_LOCAL_MEDIA_URI)
        if (localMediaUri == null) {
            WooLog.w(T.MEDIA, "productImagesService > null localMediaUri")
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
            EventBus.getDefault().post(OnProductImagesUpdateStartedEvent(Action.UPLOAD_IMAGE, remoteProductId))

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

    private fun handleRemoval(intent: Intent, remoteProductId: Long) {
        val product = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)
        if (product == null) {
            WooLog.w(T.MEDIA, "productImagesService > product is null")
            handleFailure(remoteProductId)
            return
        }

        // build a new image list containing all the product images except the passed one
        val remoteMediaId = intent.getLongExtra(KEY_REMOTE_MEDIA_ID, 0)
        val imageList = ArrayList<WCProductImageModel>()
        product.getImages().forEach { image ->
            if (image.id != remoteMediaId) {
                imageList.add(image)
            }
        }
        if (imageList.size == product.getImages().size) {
            WooLog.w(T.MEDIA, "productImagesService > product image not found")
            handleFailure(remoteProductId)
            return
        }

        currentRemovals.put(remoteProductId, remoteMediaId)

        // first fire an event that the removal is starting
        EventBus.getDefault()
                .post(OnProductImagesUpdateStartedEvent(Action.REMOVE_IMAGE, remoteProductId))

        // then dispatch the request to remove it
        val payload = UpdateProductImagesPayload(selectedSite.get(), remoteProductId, imageList)
        dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
        doneSignal.await()
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
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(currentAction, remoteProductId, isError = false))
        doneSignal.countDown()
        productImageMap.remove(remoteProductId)

        if (currentAction == Action.UPLOAD_IMAGE) {
            currentUploads.remove(remoteProductId)
        } else if (currentAction == Action.REMOVE_IMAGE) {
            currentRemovals.delete(remoteProductId)
        }
        currentAction = Action.NONE
    }

    private fun handleFailure(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(currentAction, remoteProductId, isError = true))
        doneSignal.countDown()
        currentUploads.remove(remoteProductId)
        currentAction = Action.NONE
    }
}
