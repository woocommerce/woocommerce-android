package com.woocommerce.android.ui.woopos.orders.details.refund

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches per-store v4 refund availability for the lifetime of the process.
 *
 * Keyed by the *local* site id (unique per stored site), not the remote WP.com site id, which is
 * `0` for self-hosted/WPAPI stores and would collide across them.
 */
@Singleton
class WooPosV4RefundAvailabilityCache @Inject constructor() {
    private val availabilityByLocalSiteId = ConcurrentHashMap<Int, Boolean>()

    fun isV4Available(localSiteId: Int): Boolean? = availabilityByLocalSiteId[localSiteId]

    fun markV4Available(localSiteId: Int) {
        availabilityByLocalSiteId[localSiteId] = true
    }

    fun markV4Unavailable(localSiteId: Int) {
        availabilityByLocalSiteId[localSiteId] = false
    }
}
