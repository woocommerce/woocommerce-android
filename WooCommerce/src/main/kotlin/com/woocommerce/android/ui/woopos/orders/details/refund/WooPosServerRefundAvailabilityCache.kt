package com.woocommerce.android.ui.woopos.orders.details.refund

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches per-store availability of server-computed refunds (the `/wc/v3` refund preview and
 * `compute_totals` create) for the lifetime of the process.
 *
 * Availability is only marked `true` after a successful preview probe. This is the safety gate for
 * the computed create: on stores that do not support `compute_totals`, the unknown param would be
 * silently dropped and a quantity-only body would create a ghost zero-amount refund with restock.
 *
 * Keyed by the *local* site id (unique per stored site), not the remote WP.com site id, which is
 * `0` for self-hosted/WPAPI stores and would collide across them.
 */
@Singleton
class WooPosServerRefundAvailabilityCache @Inject constructor() {
    private val availabilityByLocalSiteId = ConcurrentHashMap<Int, Boolean>()

    fun isAvailable(localSiteId: Int): Boolean? = availabilityByLocalSiteId[localSiteId]

    fun markAvailable(localSiteId: Int) {
        availabilityByLocalSiteId[localSiteId] = true
    }

    fun markUnavailable(localSiteId: Int) {
        availabilityByLocalSiteId[localSiteId] = false
    }
}
