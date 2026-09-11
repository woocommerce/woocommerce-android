package com.woocommerce.android.tools

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
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

    private val images = mutableMapOf<Long, Deferred<String?>>()
    private val requestedProductIds = mutableSetOf<Long>()

    suspend fun getAsync(remoteProductId: Long): String? {
        val request = synchronized(images) {
            val site = selectedSite.getIfExists() ?: return null
            images.getOrPut(remoteProductId) {
                val fetchIfMissing = requestedProductIds.add(remoteProductId)
                appCoroutineScope.async(dispatchers.io) {
                    loadImage(site, remoteProductId, fetchIfMissing)
                }
            }
        }
        return try {
            request.await().also { image ->
                if (image == null) {
                    synchronized(images) { images.remove(remoteProductId, request) }
                }
            }
        } finally {
            if (request.isCancelled) {
                synchronized(images) { images.remove(remoteProductId, request) }
            }
        }
    }

    private suspend fun loadImage(site: SiteModel, remoteProductId: Long, fetchIfMissing: Boolean): String? {
        productStore.getProductByRemoteId(site, remoteProductId)?.getFirstImageUrl()?.let { return it }
        if (!fetchIfMissing) return null
        val result = productStore.fetchSingleProduct(FetchSingleProductPayload(site, remoteProductId))
        return when (result.isError) {
            true -> null
            false -> productStore.getProductByRemoteId(site, remoteProductId)?.getFirstImageUrl()
        }
    }

    fun reset() {
        map.clear()
        val requests = synchronized(images) {
            images.values.toList().also {
                images.clear()
                requestedProductIds.clear()
            }
        }
        requests.forEach { it.cancel() }
    }

    fun get(remoteProductId: Long): String? {
        // first attempt to get the image URL from our map
        map[remoteProductId]?.let {
            pendingRequestIds.remove(remoteProductId)
            return it
        }

        // product isn't in our map so get it from the database
        selectedSite.getIfExists()?.let { site ->
            productStore.getProductByRemoteIdBlocking(site, remoteProductId)?.getFirstImageUrl()?.let { imageUrl ->
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
                            // Collect references to remove
                            val toRemove = mutableListOf<WeakReference<OnProductFetchedListener>>()
                            observers.forEach { weakReference ->
                                // notify the observer or collect it for removal if it's been garbage collected
                                weakReference.get()?.onProductFetched(remoteProductId) ?: toRemove.add(weakReference)
                            }
                            // Remove the collected references
                            observers.removeAll(toRemove)
                        }
                    }
                }
                // add to the list of pending requests so we don't keep fetching the same product
                pendingRequestIds.add(remoteProductId)
            }
        }

        return null
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
