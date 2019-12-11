package com.woocommerce.android.ui.imageviewer

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductImagesChanged
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductImagesPayload
import javax.inject.Inject

@OpenClassOnDebug
class ImageViewerRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val productImageMap: ProductImageMap
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun getProduct(remoteProductId: Long): Product? =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.toAppModel()

    private fun fetchProduct(remoteProductId: Long) {
        val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
    }

    /**
     * Dispatches a request to remove a single product image, returns true only if request
     * was sent
     */
    fun removeProductImage(remoteProductId: Long, remoteMediaId: Long): Boolean {
        val product = getProduct(remoteProductId)
        if (product == null) {
            WooLog.w(T.MEDIA, "removeProductImage > product is null")
            return false
        }

        // build a new image list containing all the product images except the passed one
        val imageList = product.images.filter { it.id != remoteMediaId }.map { WCProductImageModel(it.id) }

        if (product.images.size == imageList.size) {
            WooLog.w(T.MEDIA, "removeProductImage > product image not found")
            return false
        }

        // remove the image from SQLite so it's no longer available to the ui
        productStore.deleteProductImage(selectedSite.get(), remoteProductId, remoteMediaId)

        // remove this product from our image cache so the image is re-fetched the next time it's needed
        productImageMap.remove(remoteProductId)

        // then dispatch the request to remove it
        val payload = UpdateProductImagesPayload(selectedSite.get(), remoteProductId, imageList)
        dispatcher.dispatch(WCProductActionBuilder.newUpdateProductImagesAction(payload))
        return true
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductImagesChanged(event: OnProductImagesChanged) {
        // fetch the product again if the update fails - this is to restore any image we removed
        // from SQLite above
        if (event.isError) {
            fetchProduct(event.remoteProductId)
        }
    }
}
