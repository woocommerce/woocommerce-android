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
     * Returns true only when the order is confirmed to be free of subscriptions. Both "contains a
     * subscription" and "couldn't be determined" collapse to false (fail closed).
     */
    suspend fun isOrderFreeOfSubscriptions(order: Order): Boolean =
        getSubscriptionStatus(order) == SubscriptionStatus.NONE

    /**
     * Distinguishes "no subscription", "has a subscription", and "couldn't be determined" so callers
     * that can surface an error/retry (e.g. the payment method selection screen) don't confuse a
     * transient lookup failure with a genuine subscription order.
     */
    suspend fun getSubscriptionStatus(order: Order): SubscriptionStatus {
        val productIds = order.getProductIds()
        val site = selectedSite.get()
        // No products, or the plugin is inactive → the order can't be a subscription.
        if (productIds.isEmpty() || wooCommerceStore.getActiveSitePlugin(site, WOO_SUBSCRIPTIONS) == null) {
            return SubscriptionStatus.NONE
        }
        // Legacy subscription product types are known subscriptions without a network call.
        if (orderDetailRepository.hasLegacySubscriptionProducts(productIds)) return SubscriptionStatus.PRESENT

        val key = CacheKey(site.id, order.id)
        cache[key]?.let { return it.toStatus() }

        val result = subscriptionRepository.fetchSubscriptionsByOrderId(order.id, site)
        return if (result.isError) {
            // Don't cache, so a transient failure can recover on the next attempt.
            SubscriptionStatus.UNKNOWN
        } else {
            (result.model?.isEmpty() ?: false).also { cache[key] = it }.toStatus()
        }
    }

    private fun Boolean.toStatus() = if (this) SubscriptionStatus.NONE else SubscriptionStatus.PRESENT

    enum class SubscriptionStatus { NONE, PRESENT, UNKNOWN }

    private data class CacheKey(val siteId: Int, val orderId: Long)
}
