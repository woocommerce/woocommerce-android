package com.woocommerce.android.cache

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit

object SSRCache {
    private val cache = mutableMapOf<Long, CachedSSR>()
    private val mutex = Mutex()

    private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10)

    data class CachedSSR(
        val data: WCSSRModel,
        val timestamp: Long = System.currentTimeMillis()
    )

    suspend fun load(
        siteModel: SiteModel,
        wooCommerceStore: WooCommerceStore
    ): WooResult<WCSSRModel> = mutex.withLock {
        val cached = cache[siteModel.siteId]
        val now = System.currentTimeMillis()

        if (cached != null && now - cached.timestamp < CACHE_TTL_MILLIS) {
            return WooResult(cached.data)
        }

        val fetched = wooCommerceStore.fetchSSR(siteModel).model
            ?: return WooResult(WooError(WooErrorType.INVALID_RESPONSE, BaseRequest.GenericErrorType.UNKNOWN))

        cache[siteModel.siteId] = CachedSSR(fetched)
        return WooResult(fetched)
    }

    fun invalidate(siteId: Long) {
        cache.remove(siteId)
    }

    fun clear() {
        cache.clear()
    }
}
