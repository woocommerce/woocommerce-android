package com.woocommerce.android.tools

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import java.lang.ref.WeakReference
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
    private val productStore: WCProductStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
) {
    interface OnProductFetchedListener {
        fun onProductFetched(remoteProductId: Long)
    }

    private val observers: MutableList<WeakReference<OnProductFetchedListener>> = mutableListOf()

    private val map by lazy {
        HashMap<Long, String>()
    }

    private val pendingRequestIds by lazy {
        HashSet<Long>()
    }

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

            // product isn't in our database, fetch it unless there's a pending request for it
            if (!pendingRequestIds.contains(remoteProductId)) {
                appCoroutineScope.launch(dispatchers.io) {
                    // fetch the product, the method also stores it into the local database
                    val result = productStore.fetchSingleProduct(FetchSingleProductPayload(site, remoteProductId))
                    if (!result.isError) {
                        withContext(dispatchers.main) {
                            observers.forEach { weakReference ->
                                // notify the observer
                                weakReference.get()?.onProductFetched(remoteProductId)
                                    // remove the weak reference if the observer was garbage collected
                                    ?: observers.remove(weakReference)
                            }
                        }
                    }
                }
                // add to the list of pending requests so we don't keep fetching the same product
                pendingRequestIds.add(remoteProductId)
            }
        }

        return null
    }

    fun remove(remoteProductId: Long) {
        map.remove(remoteProductId)
    }

    fun subscribeToOnProductFetchedEvents(observer: OnProductFetchedListener) {
        observers.add(WeakReference(observer))
    }

    fun unsubscribeFromOnProductFetchedEvents(observer: OnProductFetchedListener): Boolean {
        observers.find { observer == it.get() }?.let {
            observers.remove(it)
            return true
        }
        return false
    }
}
