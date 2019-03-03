package com.woocommerce.android.util

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore

/**
 * Simple map of product remoteId/imageUrl used for quick lookups when attempting to display product images.
 * Note that this does *not* store the siteId so it must be cleared when the site is changed
 */
object ProductImageUrlMap {
    private val map by lazy {
        HashMap<Long, String>()
    }

    private var site: SiteModel? = null
    private var productStore: WCProductStore? = null

    fun init(site: SiteModel, productStore: WCProductStore) {
        this.site = site
        this.productStore = productStore
        map.clear()
    }

    fun get(remoteProductId: Long): String? {
        map[remoteProductId]?.let {
            return it
        }

        // product isn't in our map so get it from the store
        productStore?.let { store ->
            site?.let {
                store.getSingleProductByRemoteId(it, remoteProductId)?.getFirstImage()?.let { imageUrl ->
                    map[remoteProductId] = imageUrl
                    return imageUrl
                }
            }
        }

        return null
    }
}
