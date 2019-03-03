package com.woocommerce.android.util

import com.woocommerce.android.tools.SelectedSite
import dagger.android.AndroidInjection
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

/**
 * Maintains a map of product <remoteId, imageUrl> used for quick lookups when attempting to display
 * product images. If the product isn't in our map we load it from the db. If it's not in the db,
 * we fire an event which tells the MainPresenter to fetch it from the backend.
 */
class ProductImageUrlMap {
    private val map by lazy {
        HashMap<Long, String>()
    }

    companion object {
        private val imageUrlMap = ProductImageUrlMap()

        fun getInstance() = imageUrlMap

        fun reset() {
            getInstance().map.clear()
        }
    }

    class RequestFetchProductEvent(val site: SiteModel, val remoteProductId: Long)

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var productStore: WCProductStore

    init {
        AndroidInjection.inject(this)
    }

    fun get(remoteProductId: Long): String? {
        map[remoteProductId]?.let {
            return it
        }

        selectedSite.getIfExists()?.let { site ->
            // product isn't in our map so get it from the store
            productStore.getProductByRemoteId(site, remoteProductId)?.getFirstImage()?.let { imageUrl ->
                map[remoteProductId] = imageUrl
                return imageUrl
            }

            // product isn't in our store so fire event to fetch it
            EventBus.getDefault().post(RequestFetchProductEvent(site, remoteProductId))
        }

        return null
    }
}
