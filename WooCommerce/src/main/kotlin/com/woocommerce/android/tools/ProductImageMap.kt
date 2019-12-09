package com.woocommerce.android.tools

import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.SiteModel
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

    private val pendingRequestIds by lazy {
        HashSet<Long>()
    }

    class RequestFetchProductEvent(val site: SiteModel, val remoteProductId: Long)

    fun reset() {
        map.clear()
    }

    fun get(remoteProductId: Long): String? {
        // first attempt to get the image URL from our map
        map[remoteProductId]?.let {
            pendingRequestIds.remove(remoteProductId)
            return it
        }

        // product isn't in our map so get it from the database
        selectedSite.getIfExists()?.let { site ->
            productStore.getProductByRemoteId(site, remoteProductId)?.getFirstImageUrl()?.let { imageUrl ->
                map[remoteProductId] = imageUrl
                pendingRequestIds.remove(remoteProductId)
                return imageUrl
            }

            // product isn't in our database so fire event to fetch it unless there's a pending request for it
            if (!pendingRequestIds.contains(remoteProductId)) {
                EventBus.getDefault().post(
                        RequestFetchProductEvent(
                                site,
                                remoteProductId
                        )
                )
                // add to the list of pending requests so we don't keep fetching the same product
                pendingRequestIds.add(remoteProductId)
            }
        }

        return null
    }

    fun remove(remoteProductId: Long) {
        map.remove(remoteProductId)
    }
}
