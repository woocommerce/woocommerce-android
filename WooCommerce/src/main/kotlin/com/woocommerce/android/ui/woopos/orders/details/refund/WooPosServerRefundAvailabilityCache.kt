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
 * Each verdict records the WooCommerce version it was probed against and only answers for that
 * version, so a store that upgrades or downgrades mid-session is re-probed instead of being judged
 * by a verdict that no longer describes it. Both directions matter: a `true` carried across a
 * downgrade would reopen the ghost-refund window, and a `false` carried across an upgrade would
 * keep a now-capable store on local calculation until the process restarts.
 *
 * Keyed by the *local* site id (unique per stored site), not the remote WP.com site id, which is
 * `0` for self-hosted/WPAPI stores and would collide across them.
 */
@Singleton
class WooPosServerRefundAvailabilityCache @Inject constructor() {
    private val verdictByLocalSiteId = ConcurrentHashMap<Int, Verdict>()

    /**
     * The verdict recorded for this store while it was running [wooVersion], or null when nothing
     * has been probed for that version yet.
     */
    fun isAvailable(localSiteId: Int, wooVersion: String): Boolean? =
        verdictByLocalSiteId[localSiteId]?.takeIf { it.wooVersion == wooVersion }?.isAvailable

    fun markAvailable(localSiteId: Int, wooVersion: String) {
        verdictByLocalSiteId[localSiteId] = Verdict(isAvailable = true, wooVersion = wooVersion)
    }

    fun markUnavailable(localSiteId: Int, wooVersion: String) {
        verdictByLocalSiteId[localSiteId] = Verdict(isAvailable = false, wooVersion = wooVersion)
    }

    private data class Verdict(val isAvailable: Boolean, val wooVersion: String)
}
