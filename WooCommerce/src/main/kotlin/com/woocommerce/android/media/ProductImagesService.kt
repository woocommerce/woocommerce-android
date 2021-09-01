package com.woocommerce.android.media

import android.content.Intent
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_IMAGE_UPLOAD_FAILED
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
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
@AndroidEntryPoint
class ProductImagesService : JobIntentService() {
    companion object {
        const val ACTION_UPLOAD_IMAGES = "action_upload_images"
        const val ACTION_UPDATE_PRODUCT = "action_update_product"
        const val KEY_ID = "key_id"
        const val KEY_LOCAL_URI_LIST = "key_local_uri_list"
        const val KEY_UPLOADED_IMAGES = "key_uploaded_images"

        private const val STRIP_LOCATION = true
        private const val TIMEOUT_PER_UPLOAD = 120L

        private val canceledProducts = mutableSetOf<Long>()

        // array of ID / uploading image uris for that product
        private val currentUploads = LongSparseArray<ArrayList<Uri>>()

        // posted when the list of images starts uploading
        class OnProductImagesUpdateStartedEvent(
            val id: Long
        )

        // posted when the list of images finishes uploading
        class OnProductImagesUpdateCompletedEvent(
            val id: Long
        )

        // posted when a single image has been uploaded
        class OnProductImageUploaded(
            val localUri: Uri,
            val media: MediaModel
        )

        // posted when a single image upload failed
        class OnProductImageUploadFailed(
            val localUri: Uri,
            val media: MediaModel,
            val error: MediaError
        )

        // posted when the upload is cancelled
        class OnUploadCancelled(val productId: Long)

        fun isUploadingForProduct(id: Long): Boolean {
            return if (canceledProducts.contains(id)) {
                false
            } else {
                currentUploads.containsKey(id)
            }
        }

        fun isBusy() = !currentUploads.isEmpty

        fun getUploadingImageUris(id: Long): List<Uri>? {
            return currentUploads.get(id)
        }

        /**
         * A JobIntentService can't truly be cancelled, but we can at least set a flag that tells it
         * to stop continuing its work when the current task is done, and post an event the service
         * can use to cancel the upload
         */
        fun cancel(productId: Long) {
            canceledProducts.add(productId)
            EventBus.getDefault().post(OnUploadCancelled(productId))
        }
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var mediaFileUploadHandler: MediaFileUploadHandler
    @Inject lateinit var productDetailRepository: ProductDetailRepository
    @Inject lateinit var notifHandler: ProductImagesNotificationHandler

    private var doneSignal: CountDownLatch? = null
    private var currentMediaUpload: MediaModel? = null
    private lateinit var currentUploadUri: Uri

    override fun onCreate() {
        super.onCreate()
        WooLog.i(T.MEDIA, "productImagesService > created")
        dispatcher.register(this)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        WooLog.i(T.MEDIA, "productImagesService > destroyed")
        dispatcher.unregister(this)
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        WooLog.i(T.MEDIA, "productImagesService > onHandleWork")

        val id = intent.getLongExtra(KEY_ID, 0L)
        if (!networkStatus.isConnected()) {
            return
        }

        notifHandler.attachToService(this)

        when (intent.action) {
            ACTION_UPLOAD_IMAGES -> uploadImages(intent, id)
            ACTION_UPDATE_PRODUCT -> updateProduct(intent, id)
        }
    }

    private fun uploadImages(intent: Intent, id: Long) {
        val localUriList = intent.getParcelableArrayListExtra<Uri>(KEY_LOCAL_URI_LIST)
        if (localUriList.isNullOrEmpty()) {
            WooLog.w(T.MEDIA, "productImagesService > null media list")
            return
        }

        // set the uploads for this product
        currentUploads.put(id, localUriList)

        // post an event that the upload is starting
        val event = OnProductImagesUpdateStartedEvent(id)
        EventBus.getDefault().post(event)

        val totalUploads = localUriList.size

        canceledProducts.remove(id)

        for (index in 0 until totalUploads) {
            notifHandler.update(index + 1, totalUploads)
            currentUploadUri = localUriList[index]

            // create a media model from this local image uri
            currentMediaUpload = ProductImagesUtils.mediaModelFromLocalUri(
                this,
                selectedSite.get().id,
                currentUploadUri,
                mediaStore
            )

            if (currentMediaUpload == null) {
                WooLog.w(T.MEDIA, "productImagesService > null media")
                handleFailure(
                    mediaModel = MediaModel(),
                    mediaUploadError = MediaError(
                        MediaErrorType.NULL_MEDIA_ARG,
                        resources.getString(R.string.product_image_service_error_media_null)
                    )
                )
            } else {
                currentMediaUpload!!.postId = id
                currentMediaUpload!!.setUploadState(MediaModel.MediaUploadState.UPLOADING)

                // dispatch the upload request
                WooLog.d(T.MEDIA, "productImagesService > Dispatching request to upload $currentUploadUri")
                val payload = UploadMediaPayload(selectedSite.get(), currentMediaUpload!!, STRIP_LOCATION)
                dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))

                // wait for the upload to complete
                try {
                    doneSignal = CountDownLatch(1)
                    doneSignal!!.await(TIMEOUT_PER_UPLOAD, SECONDS)
                } catch (e: InterruptedException) {
                    WooLog.e(T.MEDIA, "productImagesService > interrupted", e)
                }
            }

            if (canceledProducts.contains(id)) {
                break
            }

            // remove this uri from the list of uploads for this product
            currentUploads.get(id)?.let { oldList ->
                val newList = ArrayList<Uri>().also {
                    it.addAll(oldList)
                    it.remove(currentUploadUri)
                }
                currentUploads.put(id, newList)
            }
        }

        notifHandler.remove()
        currentUploads.remove(id)
        productImageMap.remove(id)

        currentMediaUpload = null
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(id))
    }

    private fun updateProduct(intent: Intent, productId: Long) {
        val images = intent.getParcelableArrayListExtra<Image>(KEY_UPLOADED_IMAGES)

        notifHandler.shopUpdatingProductNotification(null)

        if (images.isNullOrEmpty()) {
            WooLog.w(T.MEDIA, "productImagesService > images passed for updating product is empty")
            return
        }

        runBlocking {
            val product = fetchProductWithRetries(productId)
            if (product == null) {
                notifHandler.postUpdateFailureNotification(productId, null)
                return@runBlocking
            }

            val result = updateProductWithRetries(product.copy(images = product.images + images))
            if (result) {
                notifHandler.postUpdateSuccessNotification(productId, product, images.size)
            } else {
                notifHandler.postUpdateFailureNotification(productId, product)
            }
        }
    }

    private suspend fun fetchProductWithRetries(productId: Long): Product? {
        var retries = 0
        while (retries < 3) {
            val product = productDetailRepository.fetchProduct(productId)
            if (product != null && productDetailRepository.lastFetchProductErrorType == null) {
                return product
            }
            retries++
        }
        return null
    }

    private suspend fun updateProductWithRetries(product: Product): Boolean {
        var retries = 0
        while (retries < 3) {
            val result = productDetailRepository.updateProduct(product)
            if (result) {
                return true
            }
            retries++
        }
        return false
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
                AnalyticsTracker.track(
                    PRODUCT_IMAGE_UPLOAD_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                    )
                )
                handleFailure(event.media, event.error)
            }
            event.canceled -> {
                WooLog.d(T.MEDIA, "productImagesService > upload media cancelled")
                countDown()
            }
            event.completed -> {
                WooLog.i(T.MEDIA, "productImagesService > uploaded media ${event.media?.id}")
                handleSuccess(event.media)
            }
            else -> {
                // otherwise this is an upload progress event
                if (!canceledProducts.contains(event.media?.postId)) {
                    val progress = (event.progress * 100).toInt()
                    notifHandler.setProgress(progress)
                }
            }
        }
    }

    private fun handleSuccess(uploadedMedia: MediaModel) {
        countDown()
        if (!canceledProducts.contains(uploadedMedia.postId)) {
            EventBus.getDefault().post(OnProductImageUploaded(currentUploadUri, uploadedMedia))
        }
    }

    private fun handleFailure(
        mediaModel: MediaModel,
        mediaUploadError: MediaError
    ) {
        countDown()
        if (!canceledProducts.contains(mediaModel.postId)) {
            EventBus.getDefault().post(OnProductImageUploadFailed(currentUploadUri, mediaModel, mediaUploadError))
        }
    }

    private fun countDown() {
        doneSignal?.let {
            if (it.count > 0) {
                it.countDown()
            }
        }
    }

    /**
     * Posted above when we want to cancel upload for a specific product
     * dispatches a request to cancel the upload
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnUploadCancelled) {
        if (event.productId == currentMediaUpload?.postId) {
            val payload = CancelMediaPayload(selectedSite.get(), currentMediaUpload, true)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
        }
    }
}
