package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.subscription.SubscriptionRepository
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether an order was sold as a subscription, which makes it ineligible for in-person
 * payment collection and Interac refunds.
 *
 * Legacy subscription product types (subscription / variable-subscription) are recognised locally
 * without a network call. Only when the products aren't a legacy subscription type do we confirm via
 * `/subscriptions?parent={orderId}`, which also catches plan-based subscriptions (a plan on any
 * product type). On subscriptions-enabled stores this fails closed: if the status can't be
 * determined (network error), the order is treated as ineligible.
 */
@Singleton
class OrderSubscriptionChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val subscriptionRepository: SubscriptionRepository,
    private val orderDetailRepository: OrderDetailRepository,
) {
    // Keyed by site + order because order IDs are not unique across sites.
    private val cache = ConcurrentHashMap<CacheKey, Boolean>()

    /**
     * Returns true only when the order is confirmed to be free of subscriptions.
     */
    suspend fun isOrderFreeOfSubscriptions(order: Order): Boolean {
        val productIds = order.getProductIds()
        if (productIds.isEmpty()) return true
        val site = selectedSite.get()
        // Plugin inactive → the order can't be a subscription.
        if (wooCommerceStore.getActiveSitePlugin(site, WOO_SUBSCRIPTIONS) == null) return true
        // Legacy subscription product types are known subscriptions without a network call.
        if (orderDetailRepository.hasLegacySubscriptionProducts(productIds)) return false

        val key = CacheKey(site.id, order.id)
        return cache[key] ?: run {
            val result = subscriptionRepository.fetchSubscriptionsByOrderId(order.id, site)
            if (result.isError) {
                // Fail closed and don't cache, so a transient failure can recover on the next attempt.
                false
            } else {
                (result.model?.isEmpty() ?: false).also { cache[key] = it }
            }
        }
    }

    private data class CacheKey(val siteId: Int, val orderId: Long)
}
