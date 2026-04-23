package com.woocommerce.android.util

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCSSRModelCachingFetcher @Inject constructor(
    private val wooCommerceStore: WooCommerceStore
) {
    private val cache = mutableMapOf<Long, CachedSSR>()
    private val mutex = Mutex()

    private val cacheTTL = TimeUnit.SECONDS.toMillis(10)

    data class CachedSSR(
        val data: WCSSRModel,
        val timestamp: Long = System.currentTimeMillis()
    )

    suspend fun load(
        siteModel: SiteModel,
        forceRefresh: Boolean = false
    ): WooResult<WCSSRModel> = mutex.withLock {
        val cached = cache[siteModel.siteId]
        val now = System.currentTimeMillis()

        if (!forceRefresh && cached != null && now - cached.timestamp < cacheTTL) {
            return WooResult(cached.data)
        }

        val fetched = wooCommerceStore.fetchSSR(siteModel).model
            ?: return WooResult(WooError(WooErrorType.INVALID_RESPONSE, BaseRequest.GenericErrorType.UNKNOWN))

        cache[siteModel.siteId] = CachedSSR(fetched)
        return WooResult(fetched)
    }

    suspend fun invalidate() = mutex.withLock {
        cache.clear()
    }
}
