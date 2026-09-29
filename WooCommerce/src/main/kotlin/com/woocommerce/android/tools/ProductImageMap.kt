package com.woocommerce.android.tools

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageMap @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
) {
    private val images = mutableMapOf<Long, Deferred<String?>>()
    private val requestedProductIds = mutableSetOf<Long>()

    suspend fun get(remoteProductId: Long): String? {
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
        val requests = synchronized(images) {
            images.values.toList().also {
                images.clear()
                requestedProductIds.clear()
            }
        }
        requests.forEach { it.cancel() }
    }
}
