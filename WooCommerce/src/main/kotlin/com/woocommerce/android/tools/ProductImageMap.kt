package com.woocommerce.android.tools

import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintains a map of product <remoteId, imageUrl> used for quick lookups when attempting to display
 * product images. If the product isn't in our map we load it from the db. If it's not in the db,
 * we fire an event which tells the MainPresenter to fetch it from the backend.
 */
@Singleton
class ProductImageMap @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    private val map by lazy {
        HashMap<Long, String>()
    }

    class RequestFetchProductEvent(val site: SiteModel, val remoteProductId: Long)

    init {
        // TODO: remove
        ProductSqlUtils.deleteProductsForSite(selectedSite.get())
    }

    fun reset() {
        map.clear()
    }

    fun get(remoteProductId: Long): String? {
        map[remoteProductId]?.let {
            return it
        }

        selectedSite.getIfExists()?.let { site ->
            // product isn't in our map so get it from the database
            productStore.getProductByRemoteId(site, remoteProductId)?.getFirstImage()?.let { imageUrl ->
                map[remoteProductId] = imageUrl
                return imageUrl
            }

            // product isn't in our database so fire event to fetch it
            EventBus.getDefault().post(
                    RequestFetchProductEvent(
                            site,
                            remoteProductId
                    )
            )
        }

        return null
    }
}
