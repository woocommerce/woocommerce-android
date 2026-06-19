package com.woocommerce.android.ui.woopos.orders.details.refund

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches, per site for the lifetime of the app process, whether the v4 refund endpoints are
 * available on the store.
 *
 * The v4 refund preview/create endpoints are behind a feature flag on the store side. Probing them
 * on every refund would add latency and noise, so the result of the first probe (a `404
 * rest_no_route` means unavailable, a successful call means available) is cached and keyed by
 * remote site id, so switching stores does not reuse a stale result.
 */
@Singleton
class WooPosV4RefundAvailabilityCache @Inject constructor() {
    private val availabilityBySiteId = ConcurrentHashMap<Long, Boolean>()

    /**
     * @return `true` if v4 is known available, `false` if known unavailable, `null` if not probed yet.
     */
    fun isV4Available(siteId: Long): Boolean? = availabilityBySiteId[siteId]

    fun markV4Available(siteId: Long) {
        availabilityBySiteId[siteId] = true
    }

    fun markV4Unavailable(siteId: Long) {
        availabilityBySiteId[siteId] = false
    }
}
