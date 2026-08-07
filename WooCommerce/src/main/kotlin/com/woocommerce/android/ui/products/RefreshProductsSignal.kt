package com.woocommerce.android.ui.products

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide signal used by order flows (payment, fulfillment, status change, refund, create/edit) to tell the Products
 * list that a set of products was affected by a server-side order change and should be re-fetched. The list reacts by
 * refreshing only those products, so a stale value (e.g. stock) is corrected without a manual pull-to-refresh and
 * without resetting pagination. Emitters signal after the server confirms the change; some may emit even when a
 * product's stock didn't actually change, and the refresh re-fetches the whole product rather than just its stock —
 * that's harmless, the affected rows are simply re-read.
 *
 * Ids are *accumulated* in a set rather than emitted as one-shot events, so nothing is lost when the Products list
 * isn't collecting (e.g. it isn't open, or it's busy processing a previous batch) and repeated ids are deduped.
 * The collector drains the set via [clearProcessed] once it has refreshed those products.
 *
 * Note: because pending state is presence-only, two changes for the *same* product within a single refresh
 * round-trip are coalesced; the rare "second change lands and is cleared before it's fetched" case self-heals on the
 * next change for that product or a manual refresh. Ids left pending while offline are retried the next time
 * the set changes or a new [ProductListViewModel][ui.products.list.ProductListViewModel] instance starts collecting.
 */
@Singleton
class RefreshProductsSignal @Inject constructor() {
    private val _pendingProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val pendingProductIds: StateFlow<Set<Long>> = _pendingProductIds.asStateFlow()

    fun notifyProductsChanged(productIds: List<Long>) {
        if (productIds.isNotEmpty()) {
            _pendingProductIds.update { it + productIds }
        }
    }

    fun clearProcessed(productIds: Set<Long>) {
        _pendingProductIds.update { it - productIds }
    }
}
