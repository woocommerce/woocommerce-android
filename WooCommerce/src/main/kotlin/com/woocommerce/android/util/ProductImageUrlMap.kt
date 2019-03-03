package com.woocommerce.android.util

import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore

/**
 * Maintains a map of product <remoteId, imageUrl> used for quick lookups when attempting to display
 * product images. If the product isn't in our map we load it from the db. If it's not in the db,
 * we fire an event which is handled by the MainPresnter to fetch it from the backend
 */
object ProductImageUrlMap {
    private val map by lazy {
        HashMap<Long, String>()
    }

    class RequestFetchProductEvent(val site: SiteModel, val remoteProductId: Long)

    private var site: SiteModel? = null
    private var productStore: WCProductStore? = null

    /**
     * Must be called at startup
     */
    fun init(site: SiteModel, productStore: WCProductStore) {
        this.site = site
        this.productStore = productStore
    }

    /**
     * Must be called whenever the selected site has changed
     */
    fun reset(site: SiteModel) {
        this.site = site
        map.clear()
    }

    fun get(remoteProductId: Long): String? {
        map[remoteProductId]?.let {
            return it
        }

        // product isn't in our map so get it from the store
        productStore?.let { store ->
            site?.let { siteModel ->
                store.getProductByRemoteId(siteModel, remoteProductId)?.getFirstImage()?.let { imageUrl ->
                    map[remoteProductId] = imageUrl
                    return imageUrl
                }

                // product isn't in our store so fire event to fetch it
                EventBus.getDefault().post(RequestFetchProductEvent(siteModel, remoteProductId))
            }
        }

        return null
    }
}
