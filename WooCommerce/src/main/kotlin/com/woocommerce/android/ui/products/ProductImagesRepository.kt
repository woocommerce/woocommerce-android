package com.woocommerce.android.ui.products

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
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
class ProductImagesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
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
        val imageList = ArrayList<WCProductImageModel>()
        var removedImage: WCProductImageModel? = null
        product.images.forEach { image ->
            if (image.id == remoteMediaId) {
                removedImage = image
            } else {
                imageList.add(image)
            }
        }
        if (removedImage == null) {
            WooLog.w(T.MEDIA, "removeProductImage > product image not found")
            return false
        }

        // remove the image from SQLite so it's no longer available to the ui
        productStore.deleteProductImage(selectedSite.get(), remoteProductId, remoteMediaId)

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
