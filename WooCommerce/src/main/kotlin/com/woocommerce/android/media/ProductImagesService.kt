package com.woocommerce.android.media

import android.content.Intent
import android.net.Uri
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_IMAGE_UPLOAD_FAILED
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
class ProductImagesService : JobIntentService() {
    companion object {
        const val KEY_ID = "key_id"
        const val KEY_LOCAL_URI_LIST = "key_local_uri_list"

        private const val STRIP_LOCATION = true
        private const val TIMEOUT_PER_UPLOAD = 120L

        private var isCancelled: Boolean = false

        // array of ID / uploading image uris for that product
        private val currentUploads = LongSparseArray<ArrayList<Uri>>()

        // posted when the list of images starts uploading
        class OnProductImagesUpdateStartedEvent(
            val id: Long
        )

        // posted when the list of images finishes uploading
        class OnProductImagesUpdateCompletedEvent(
            val id: Long,
            val isCancelled: Boolean
        )

        // posted when a single image has been uploaded
        class OnProductImageUploaded(
            val media: MediaModel? = null,
            val isError: Boolean = false
        )

        // posted when the upload is cancelled
        class OnUploadCancelled

        fun isUploadingForProduct(id: Long): Boolean {
            return if (isCancelled) {
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
        fun cancel() {
            isCancelled = true
            EventBus.getDefault().post(OnUploadCancelled())
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
    private var currentMediaUpload: MediaModel? = null
    private lateinit var notifHandler: ProductImagesNotificationHandler

    override fun onCreate() {
        WooLog.i(T.MEDIA, "productImagesService > created")
        AndroidInjection.inject(this)
        dispatcher.register(this)
        EventBus.getDefault().register(this)
        super.onCreate()
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
        notifHandler = ProductImagesNotificationHandler(this, id)

        isCancelled = false

        for (index in 0 until totalUploads) {
            notifHandler.update(index + 1, totalUploads)
            val localUri = localUriList[index]

            // create a media model from this local image uri
            currentMediaUpload = ProductImagesUtils.mediaModelFromLocalUri(
                    this,
                    selectedSite.get().id,
                    localUri,
                    mediaStore
            )

            if (currentMediaUpload == null) {
                WooLog.w(T.MEDIA, "productImagesService > null media")
                handleFailure()
            } else {
                currentMediaUpload!!.postId = id
                currentMediaUpload!!.setUploadState(MediaModel.MediaUploadState.UPLOADING)

                // dispatch the upload request
                WooLog.d(T.MEDIA, "productImagesService > Dispatching request to upload $localUri")
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

            if (isCancelled) {
                break
            }

            // remove this uri from the list of uploads for this product
            currentUploads.get(id)?.let { oldList ->
                val newList = ArrayList<Uri>().also {
                    it.addAll(oldList)
                    it.remove(localUri)
                }
                currentUploads.put(id, newList)
            }
        }

        if (isCancelled) {
            currentUploads.clear()
        } else {
            notifHandler.remove()
            currentUploads.remove(id)
            productImageMap.remove(id)
        }

        currentMediaUpload = null
        EventBus.getDefault().post(OnProductImagesUpdateCompletedEvent(id, isCancelled))
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
                    PRODUCT_IMAGE_UPLOAD_FAILED, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))
                handleFailure()
            }
            event.canceled -> {
                WooLog.d(T.MEDIA, "productImagesService > upload media cancelled")
            }
            event.completed -> {
                WooLog.i(T.MEDIA, "productImagesService > uploaded media ${event.media?.id}")
                handleSuccess(event.media)
            } else -> {
                // otherwise this is an upload progress event
                if (!isCancelled) {
                    val progress = (event.progress * 100).toInt()
                    notifHandler.setProgress(progress)
                }
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

    /**
     * Posted above when we want the upload cancelled - removes the upload notification and
     * dispatches a request to cancel the upload
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnUploadCancelled) {
        notifHandler.remove()
        doneSignal?.let {
            while (it.count > 0) {
                it.countDown()
            }
        }

        currentMediaUpload?.let {
            val payload = CancelMediaPayload(selectedSite.get(), it, true)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
        }
    }
}
