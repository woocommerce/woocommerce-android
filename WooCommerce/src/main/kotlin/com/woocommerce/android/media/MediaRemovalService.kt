package com.woocommerce.android.media

import android.content.Intent
import androidx.collection.LongSparseArray
import androidx.core.app.JobIntentService
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

/**
 * service which removes a product image - note that this does not remove the image from
 * the WP media library, it only removes it from the product
 */
class MediaRemovalService : JobIntentService() {
    companion object {
        const val KEY_REMOTE_PRODUCT_ID = "key_remote_product_id"
        const val KEY_REMOTE_MEDIA_ID = "key_remote_media_id"

        // array of remoteProductId / remoteMediaId
        private val currentRemovals = LongSparseArray<Long>()

        class OnProductImageRemovalStartedEvent(
            var remoteProductId: Long,
            var remoteMediaId: Long
        )

        class OnProductImageRemovalCompletedEvent(
            var remoteProductId: Long,
            val isError: Boolean
        )
        
        
        fun isRemovingProductImage(remoteProductId: Long, remoteMediaId: Long) =
                currentRemovals[remoteProductId] == remoteMediaId

        fun isBusy() = !currentRemovals.isEmpty
    }

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var mediaStore: MediaStore
    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productImageMap: ProductImageMap

    private val doneSignal = CountDownLatch(1)

    override fun onCreate() {
        WooLog.i(WooLog.T.MEDIA, "MediaRemovalService > created")
        AndroidInjection.inject(this)
        dispatcher.register(this)
        super.onCreate()
    }

    override fun onDestroy() {
        WooLog.i(WooLog.T.MEDIA, "MediaRemovalService > destroyed")
        dispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onHandleWork(intent: Intent) {
        WooLog.i(WooLog.T.MEDIA, "MediaRemovalService > onHandleWork")

        val remoteProductId = intent.getLongExtra(KEY_REMOTE_PRODUCT_ID, 0L)
        val remoteMediaId = intent.getLongExtra(KEY_REMOTE_MEDIA_ID, 0)
        currentRemovals.put(remoteProductId, remoteMediaId)
        dispatchRemoveMediaAction(remoteProductId, remoteMediaId)
    }

    override fun onStopCurrentWork(): Boolean {
        super.onStopCurrentWork()
        WooLog.i(WooLog.T.MEDIA, "MediaRemovalService > onStopCurrentWork")
        return true
    }

    /**
     * Dispatches a request to remove a single image from a product
     */
    private fun dispatchRemoveMediaAction(remoteProductId: Long, remoteMediaId: Long) {
        val product = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)
        if (product == null) {
            WooLog.w(WooLog.T.MEDIA, "MediaUploadService > product is null")
            handleFailure(remoteProductId)
            return
        }

        // build a new image list containing all the product images except the passed one
        val imageList = ArrayList<WCProductImageModel>()
        product.getImages().forEach { image ->
            if (image.id != remoteMediaId) {
                imageList.add(image)
            }
        }
        if (imageList.size == product.getImages().size) {
            WooLog.w(WooLog.T.MEDIA, "MediaRemovalService > product image not found")
            handleFailure(remoteProductId)
            return
        }

        // first fire an event that the upload is starting
        EventBus.getDefault().post(OnProductImageRemovalStartedEvent(remoteProductId, remoteMediaId))

        // then dispatch the request to remove it
        val payload = UpdateProductImagesPayload(selectedSite.get(), remoteMediaId, imageList)
        dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        if (event.isError) {
            WooLog.w(
                    WooLog.T.MEDIA,
                    "MediaUploadService > error changing product images: ${event.error.type}, ${event.error.message}"
            )
            handleFailure(event.remoteProductId)
        } else {
            WooLog.i(WooLog.T.MEDIA, "MediaUploadService > product images changed")
            handleSuccess(event.remoteProductId)
        }
    }

    private fun handleSuccess(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductImageRemovalCompletedEvent(remoteProductId, isError = false))
        doneSignal.countDown()
        productImageMap.remove(remoteProductId)
        currentRemovals.delete(remoteProductId)
    }

    private fun handleFailure(remoteProductId: Long) {
        EventBus.getDefault().post(OnProductImageRemovalCompletedEvent(remoteProductId, isError = true))
        doneSignal.countDown()
        productImageMap.remove(remoteProductId)
        currentRemovals.delete(remoteProductId)
    }
}
